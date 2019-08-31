(defproject me.arrdem/lein-git-version "_"
  :description "Use git for project versions."
  :url "https://github.com/arrdem/lein-git-version"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :eval-in-leiningen true
  :dependencies [[me.arrdem/cuddlefish "0.1.0"]]
  :deploy-repositories [["releases" :clojars]]

  :plugins [[me.arrdem/lein-git-version "LATEST"]]
  :git-version
  ,,{:status-to-version lein-git-version.plugin/default-status-to-version})
