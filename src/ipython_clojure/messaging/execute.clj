(ns ipython-clojure.messaging.execute
  (:require [ipython-clojure.messaging.message-proto :refer :all]
            [ipython-clojure.messaging.utils :refer :all]
            [clojure.tools.nrepl :as repl]))

(def execution-count (atom 0N))

(def nrepl-session (atom ""))

(defn status-content [status]
  {:execution_state status})

(defn pyin-content [execution-count message]
  {:execution_count execution-count
   :code (get-in message [:content :code])})

(defn prepare-resp
  [resp]
  (->> resp (map repl/read-response-value) repl/combine-responses))

(defn execute [repl-conn request]
  (let [resp (repl/message (repl/client repl-conn 1000) {:op "eval" :code (get-in request [:content :code])})
        prepared-resp (prepare-resp resp)]
    (swap! nrepl-session (fn [_] (-> resp first :session)))
    (if (:value prepared-resp)
      {:value (first (:value prepared-resp)) :out (:out prepared-resp)}
      (select-keys prepared-resp [:err :root-ex :ex :out]))))

(defn execute-reply-message
  [execute-result]
  (if (:err execute-result)
    {:status "error"
     :execution_count @execution-count
     :ename (:ex execute-result)
     :evalue (:err execute-result)
     :traceback [(:err execute-result)]}
    {:status "ok"
     :execution_count @execution-count
     :payload [{:source "page" :data {:text/plain (str (:value execute-result))} :start 0}]
     :user_expressions {}}))

(defn concatenate-out-on-front
  [execute-result]
  (let [original-value (if (:err execute-result) (:err execute-result) (:value execute-result))]
    (if (:out execute-result)
      (str (:out execute-result) original-value)
      (str original-value))))

(defn send-out-message!
  [iopub-socket username parent-header session-id execute-result]
  (if (:err execute-result)
    (do
      (send-message iopub-socket "error" username
                  {:execution_count @execution-count
                   :ename (:ex execute-result)
                   :evalue (:err execute-result)
                   :traceback []}
                  parent-header {} session-id)
      (send-message iopub-socket "execute_result" username
                    {:execution_count @execution-count
                     :data {:text/plain (concatenate-out-on-front execute-result)}
                     :metadata {}}
                    parent-header {} session-id))
    (send-message iopub-socket "execute_result" username
                  {:execution_count @execution-count
                   :data {:text/plain (concatenate-out-on-front execute-result)}
                   :metadata {}}
                  parent-header {} session-id)))

(defmethod reply-to-message "execute_request"
  [message shell-socket iopub-socket nrepl-conn]
  (let [session-id (get-in message [:header :session])
        username (get-in message [:header :username])
        parent-header (:header message)
        execute-result (execute nrepl-conn message)]
    (swap! execution-count inc)
    (send-message iopub-socket "status" username (status-content "busy")
                  parent-header {} session-id)
    (send-message iopub-socket "execute_input" username (pyin-content @execution-count message)
                  parent-header {} session-id)
    (when (:out execute-result)
      (send-message shell-socket "stream" username {:name "stdout" :text (:out execute-result)}
                    parent-header {} session-id))
    (send-message shell-socket "execute_reply" username
                  (execute-reply-message execute-result)
                  parent-header
                  {:dependencies_met "True"
                   :engine session-id
                   :status "ok"
                   :started (now)} session-id)
    (send-out-message! iopub-socket username parent-header session-id execute-result)
    (send-message iopub-socket "status" username (status-content "idle")
                  parent-header {} session-id)))


