(ns ipython-clojure.messaging.shutdown
  (:require [ipython-clojure.messaging.message-proto :refer :all]))

(defmethod reply-to-message "shutdown_request"
  [_ _ _ _]
  (println "Exiting Clojure Kernel")
  (System/exit 0))
