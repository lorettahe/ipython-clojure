(ns ipython-clojure.messaging.message-proto)

(defmulti reply-to-message
  (fn [message shell-socket iopub-socket repl-conn] (get-in message [:header :msg_type]))
  :default :not-implemented)

(defmethod reply-to-message :not-implemented
  [message _ _ _]
  (println "Messaging protocol not yet implemented for message type" (get-in message [:header :msg-type])))
