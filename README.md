# lein-git-version

[![Clojars Project](https://img.shields.io/clojars/v/me.arrdem/lein-git-version.svg)](https://clojars.org/me.arrdem/lein-git-version)

Derive your Leiningen project version from your Git history.

## Motivation

Leiningen projects, in their heritage from Maven, list an explicit
version as the 3rd element of a `project.clj` file. For instance

```clojure
(defproject foo "1.3.4"
  ...)
```

There are a couple of problems with this.  First of all, until the
arrival of `leiningen.release/bump-version` for the `lein release`
task, there was really no sane way to update the version of a property
short of a sed script to rewrite the `project.clj`.

While `bump-version` is a mostly acceptable solution, it still relies
on storing the version identifier in a file, attached to some
point in the history of the repository.

The problem with sticking a version identifier in the filesystem/version
history is that it becomes a source of merge conflicts. To avoid the
merge conflicts, teams sometimes build complicated merge hooks or
automated workflows. Now you have two problems.

In monorepo patterns ala
[lein-modules](https://github.com/jcrossley3/lein-modules), the problems
above become amplified. Additionally, versions for inter-monorepo dependencies
are not very meaningful as all dependencis are built from the same source
commit, rather than depending on a specific Maven version identifier.
Using a commit ID or version control tag/label is a more accurate and useful.

## Usage

Add:

```clojure
;; Add in the git-version plugin
:plugins [[me.arrdem/lein-git-version "2.0.3"]
          ...]
```

By default, my incarnation of lein-git-version doesn't do anything to
your project, except make some additional keys visible in the project
map.

```clojure
{:tag       ;; Name of the last git tag if any
 :ahead     ;; Number of commits ahead of the last tag, or 0
 :ahead?    ;; Is the head ahead by more than 0 commits
 :ref       ;; The full current ref
 :ref-short ;; The "short" current ref
 :branch    ;; The name of the current branch
 :dirty?    ;; Optional. Boolean. Are there un-committed changes.
 :message   ;; Optional. The last commit message when clean.
 :timestamp ;; Optional. The last commit date when clean.
}
```
If the keyword `:project/git-ref` or `:project/git-ref-short` is used in place
of a version string, lein-git-version will update the version string
of the project to instead reflect the ref or short ref.

For instance,

```clojure
(defproject bar :project/git-ref
  :plugins [[me.arrdem/lein-git-version "2.0.3"]]
  ...)
```
or

```clojure
(defproject baz :project/git-ref-short
  :plugins [[me.arrdem/lein-git-version "2.0.3"]]
  ...)
```
lein-git-version can also be used to compute a versions string, as an
arbitrary function of the current git status by specifying a
`status-to-version` function of the above status structure in the
`:git-version` map of your `project.clj`.

For instance, lein-git-version uses an earlier version of
itself to compute its own version.

```clojure
(defproject me.arrdem/lein-git-version "_"
  :plugins [[me.arrdem/lein-git-version "2.0.3"]]

  :git-version
    {:status-to-version
       (fn [{:keys [tag version branch ahead ahead? dirty?] :as git}]
          (assert (re-find #"\d+\.\d+\.\d+" tag)
                  "Tag is assumed to be a raw SemVer version")
          (if (and tag (not ahead?) (not dirty?))
             tag
             (let [[_ prefix patch] (re-find #"(\d+\.\d+)\.(\d+)" tag)
                   patch            (Long/parseLong patch)
                   patch+           (inc patch)
                   branch+          (-> branch
                                       (.replaceAll "[^A-Za-z0-9]" "_"))]
               (format "%s.%d-%s-SNAPSHOT" prefix patch+ branch+))))
  }
  ...)
```

Will use the last tag as the version if level with a tag, otherwise
will produce a `SNAPSHOT` one SemVer patch ahead of the previous tag
qualified with the name of the current branch.

This enables your release workflow to consist simply of creating a tag
and doing a deploy. No source changes are required.

Like its predecessors, lein-git-version can be used to generate a
version file. Previous incarnations tried to lay down a version
namespace. This incarnation can be configured to lay down an EDN file
containing git information at a specified location relative to the
root of the project.

For instance,

```clojure
(defproject com.my-app/cares-what-version-it-is :project/ref-short
  :plugins [[me.arrdem/lein-git-version "2.0.3"]]
  ...
  :git-version {:version-file "resources/com/my_app/version.edn"
                :version-file-keys [:ref :version :timestamp]}))
```

will cause lein-git-version to make the specified directory, and lay
down an EDN file containing all the selected project status
information from the project map.

It's suggested that rather than target a directory under source
control, a source control ignored eg `"gen-resources/..."` directory
be used to isolate generated build artifacts from user
sources. Remember to add the target to your project's `:resource-paths`!

## History

This repo is a fork and to my taste a massive cleanup of
[cvillecsteele's lein-git-version](https://github.com/cvillecsteele/lein-git-version)
which is itself an un-maintained alternative to
[michalmarczyk's](https://github.com/michalmarczyk/lein-git-version)
original project.

## License

Copyright © 2017 Reid "arrdem" McKenzie

Derived from lein-git-version © 2016 Colin Steele

Derived from lein-git-version © 2011 Michał Marczyk

Distributed under the Eclipse Public License, the same as Clojure.
