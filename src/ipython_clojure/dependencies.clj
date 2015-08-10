(ns ipython-clojure.dependencies
  (:require [leiningen.core.project :as project]
            [leiningen.core.classpath :as classpath]
            [clojure.java.io :as io]))

(defn project-file-exists?
  []
  (let [project-file (io/file "project.clj")]
    (if (.exists project-file)
      project-file
      nil)))

(defn read-raw-project-clj
  []
  (project/read-raw "project.clj"))

(defn get-project-classpath
  []
  (if (project-file-exists?)
    (let [proj (read-raw-project-clj)]
      (classpath/get-classpath proj))
    []))




