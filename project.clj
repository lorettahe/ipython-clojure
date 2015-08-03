(defproject ipython-clojure "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.zeromq/jeromq "0.3.5"]
                 [cheshire "5.3.1"]
                 [clj-time "0.7.0"]
                 [org.zeromq/cljzmq "0.1.4" :exclusions [org.zeromq/jzmq]]
                 [org.clojure/data.json "0.2.4"]
                 [org.clojure/tools.nrepl "0.2.10"]
                 [cider/cider-nrepl "0.9.1"]
                 [digest "1.4.4"]]
  :aot [ipython-clojure.core]
  :main ipython-clojure.core
  :jvm-opts ["-Xmx250m"]
  :keep-non-project-classes true)
