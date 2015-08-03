(ns ipython-clojure.messaging.history
  (:require [ipython-clojure.messaging.message-proto :refer :all]
            [ipython-clojure.messaging.utils :refer :all]))

(defmethod reply-to-message "history_request"
  [message shell-socket iopub-socket repl-client]
  (send-message
    shell-socket
    "history_reply"
    (get-in message [:header :username])
    []
    (:header message)
    {}
    (get-in message [:header :session])))
