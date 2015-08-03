(ns ipython-clojure.messaging.kernel-info
  (:require [ipython-clojure.messaging.message-proto :refer :all]
            [ipython-clojure.messaging.utils :refer :all]))

(def kernel-info-content
  {:protocol_version     "5.0"
   :implementation         "Clojure"
   :implementation_version "1.0.0"
   :language_info
                         {:name             "clojure"
                          :version          "1.5.1"
                          :mimetype         "text/plain"
                          :file_extension   ".clj"}})

(defmethod reply-to-message "kernel_info_request"
  [message shell-socket iopub-socket repl-client]
  (send-message
    shell-socket
    "kernel_info_reply"
    (get-in message [:header :username])
    kernel-info-content
    (:header message)
    {}
    (get-in message [:header :session])))
