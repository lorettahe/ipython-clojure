(ns ipython-clojure.messaging.execute
  (:require [ipython-clojure.messaging.message-proto :refer :all]
            [ipython-clojure.messaging.utils :refer :all]
            [clojure.tools.nrepl :as repl]
            [clojure.tools.nrepl.middleware.session :refer [session]]))

(def execution-count (atom 0N))

(def nrepl-session (atom nil))

(defn status-content [status]
  {:execution_state status})

(defn pyin-content [execution-count message]
  {:execution_count execution-count
   :code (get-in message [:content :code])})

(defn safe-read-response-value
  [{:keys [value] :as msg}]
  (if-not (string? value)
    msg
    (try
      (assoc msg :value (read-string value))
      (catch Exception e
        (assoc msg :value (str value))))))

(defn prepare-resp
  [resp]
  (->> resp (map safe-read-response-value) repl/combine-responses))

(defn execute [repl-client request]
  (let [resp (repl/message repl-client {:op "eval" :code (get-in request [:content :code])})
        prepared-resp (prepare-resp resp)]
    (swap! nrepl-session (fn [_] (-> resp first :session)))
    (if (:value prepared-resp)
      {:value (first (:value prepared-resp)) :out (:out prepared-resp)}
      (select-keys prepared-resp [:err :root-ex :ex :out]))))

(defn execute-reply-message
  [execute-result exception-stacktrace]
  (if (:err execute-result)
    {:status "error"
     :execution_count @execution-count
     :ename (:ex execute-result)
     :evalue (:err execute-result)
     :traceback [exception-stacktrace]}
    {:status "ok"
     :execution_count @execution-count
     :payload [{:source "page" :data {:text/plain (str (:value execute-result))} :start 0}]
     :user_expressions {}}))

(defn send-out-message!
  [iopub-socket username parent-header session-id execute-result exception-stacktrace]
  (if (:err execute-result)
    (send-message iopub-socket "error" username
                  {:execution_count @execution-count
                   :ename           (:ex execute-result)
                   :evalue          (:err execute-result)
                   :traceback       [exception-stacktrace]}
                  parent-header {} session-id)
    (send-message iopub-socket "execute_result" username
                  {:execution_count @execution-count
                   :data {:text/plain (str (:value execute-result))}
                   :metadata {}}
                  parent-header {} session-id)))

(defn get-exception!
  [repl-client]
  (let [resp (repl/message repl-client {:op "eval" :code "(println *e)"})
        prepared-resp (prepare-resp resp)]
    (:out prepared-resp)))

(defmethod reply-to-message "execute_request"
  [message shell-socket iopub-socket repl-client]
  (let [session-id (get-in message [:header :session])
        username (get-in message [:header :username])
        parent-header (:header message)
        execute-result (execute repl-client message)
        exception-stacktrace (when (:err execute-result) (get-exception! repl-client))]
    (swap! execution-count inc)
    (send-message iopub-socket "status" username (status-content "busy")
                  parent-header {} session-id)
    (send-message iopub-socket "execute_input" username (pyin-content @execution-count message)
                  parent-header {} session-id)
    (when-not (nil? (:out execute-result))
      (send-message iopub-socket "stream" username {:name "stdout" :text (:out execute-result)}
                    parent-header {} session-id))
    (send-message shell-socket "execute_reply" username
                  (execute-reply-message execute-result exception-stacktrace)
                  parent-header
                  {} session-id)
    (send-out-message! iopub-socket username parent-header session-id execute-result exception-stacktrace)
    (send-message iopub-socket "status" username (status-content "idle")
                  parent-header {} session-id)))


