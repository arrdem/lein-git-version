(ns lein-git-version.plugin
  "The lein-git-version plugin as loaded by lein itself."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [with-sh-dir]]
            [cuddlefish.core :as git]))

(def default-config
  "The default configuration values."
  {:git               "git"
   :describe-pattern  git/git-describe-pattern
   :status-to-version    nil
   :version-file      nil
   :version-file-keys [:ref :ref-short :tag :ahead :ahead? :dirty? :message :timestamp :version]})

(defn default-status-to-version
  "A function you may want use for :status-to-version"
  [{:keys [tag version branch ahead ahead? dirty?] :as git}]
          (assert (re-find #"\d+\.\d+\.\d+" tag)
                  "Tag is assumed to be a raw SemVer version")
          (if (and tag (not ahead?) (not dirty?))
             tag
             (let [[_ prefix patch] (re-find #"(\d+\.\d+)\.(\d+)" tag)
                   patch            (Long/parseLong patch)
                   patch+           (inc patch)]
               (format "%s.%d-%s-SNAPSHOT" prefix patch+ branch))))

(defn write-version-file
  "Write a project \"version\" file."
  [{:keys [root] :as project} file keys]
  (let [f (io/file root file)]
    (io/make-parents f)
    (with-open [writer (io/writer f)]
      (binding [*out* writer]
        (prn (select-keys project keys)))))

  ;; Don't transform the project so as to fit in the cond-> pipeline
  project)

(defn middleware
  "Leiningen middleware.

  Function of a project to a project which inserts git status and ref information.

  Tries to unconditionally create the `:ref`, `:ref-short`, `:tag`,
  `:ahead`, `:ahead?` and `:dirty?` keys in the leiningen project
  map. May create the `:message` and `:timestamp` keys as well.

  If the project's `:version` is `:project/ref` or
  `:project/ref-short`, the git ref or short ref will be used as the
  version of the project.

  If the user has specified a `:status-to-version` lambda under the
  `:git-version` configuration map, that function will be used to
  compute a project version based on the current git state."

  [{:keys           [git-version name root]
    project-version :version
    :as             project}]
  (with-sh-dir root
    (let [{:keys [version-file version-file-keys status-to-version] :as config}
    ,,(merge default-config git-version)

          branch (git/current-branch config)

          {:keys [tag version ahead ahead? ref ref-short] :as status}
          ,,(git/status config)

          status
          ,,(-> status
                (dissoc :version)
                (assoc :branch branch))

          status-to-version
          ,,(when status-to-version
              (try (eval status-to-version)
                   (catch Exception e
                     (binding [*out* *err*]
                       (printf (str "While trying to evaluate status-to-version function:\n%s\n\n"
                                    "Encountered exception:\n%s\n")
                               status-to-version e))
                     (System/exit 1))))]
      (cond-> (merge project status)
              (= project-version :project/git-ref-short)
              ,,(assoc :version ref-short)

              (= project-version :project/git-ref)
              ,,(assoc :version ref)

              status-to-version
              ,,(assoc :version (status-to-version status))

              version-file
              ,,(write-version-file version-file version-file-keys)))))
