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

(defn pyout-content [execution-count execute-result]
  {:execution_count execution-count
   :data {:text/plain (str execute-result)}
   :metadata {}})

(defn execute [repl-conn request]
  (let [resp (repl/message (repl/client repl-conn 1000) {:op "eval" :code (get-in request [:content :code])})]
    (println resp)
    (swap! nrepl-session (fn [_] (-> resp first :session)))
    (first (repl/response-values resp))))

(defmethod reply-to-message "execute_request"
  [message shell-socket iopub-socket nrepl-conn]
  (let [session-id (get-in message [:header :session])
        username (get-in message [:header :username])
        parent-header (:header message)
        execute-result (execute nrepl-conn message)]
    (swap! execution-count inc)
    (send-message iopub-socket "status" username (status-content "busy")
                  parent-header {} session-id)
    (send-message iopub-socket "pyin" username (pyin-content @execution-count message)
                  parent-header {} session-id)
    (send-message shell-socket "execute_reply" username
                  {:status "ok"
                   :execution_count @execution-count
                   :user_variables {}
                   :payload [{:source "page" :data {:text/plain (str execute-result)} :start 0}]
                   :user_expressions {}}
                  parent-header
                  {:dependencies_met "True"
                   :engine session-id
                   :status "ok"
                   :started (now)} session-id)
    (send-message iopub-socket "pyout" username (pyout-content @execution-count execute-result)
                  parent-header {} session-id)
    (send-message iopub-socket "status" username (status-content "idle")
                  parent-header {} session-id)))


