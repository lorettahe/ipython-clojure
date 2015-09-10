(ns ipython-clojure.messaging.execute
  (:require [ipython-clojure.messaging.message-proto :refer :all]
            [ipython-clojure.messaging.utils :refer :all]
            [clojure.tools.nrepl :as repl]
            [clojure.tools.nrepl.middleware.session :refer [session]]
            [ipython-clojure.chart-utils :refer [get-chart-png-bytes]])
  (:import [org.jfree.chart ChartUtilities JFreeChart]))

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
  (if (and (get-in request [:content :code]) (= "exit" (.trim (get-in request [:content :code]))))
    :exit
    (let [resp (repl/message repl-client {:op "eval" :code (get-in request [:content :code])})
          prepared-resp (prepare-resp resp)]
      (swap! nrepl-session (fn [_] (-> resp first :session)))
      (cond
        (and (:value prepared-resp) (= :chart (:ipython-clojure.chart-utlls/type (first (:value prepared-resp)))))
        {:value (:ipython-clojure.chart-utils/data (first (:value prepared-resp))) :out (:out prepared-resp) :type :chart}

        (:value prepared-resp)
        {:value (first (:value prepared-resp )) :out (:out prepared-resp) :type :text}

        :else
        (select-keys prepared-resp [:err :root-ex :ex :out])))))

(defn execute-reply-message
  [execute-result exception-stacktrace]
  (cond
    (= :exit execute-result)
    {:status "ok" :execution_count @execution-count :payload [{:source "ask_exit" :keepkernel false}]}

    (:err execute-result)
    {:status "error" :execution_count @execution-count :ename (:ex execute-result) :evalue (:err execute-result) :traceback [exception-stacktrace]}

    (= :chart (:type execute-result))
    (do
      {:status "ok" :execution_count @execution-count :payload [{:source "page" :data {:image/png (:value execute-result)} :start 0}] :user_expressions {}})

    :else
    {:status "ok" :execution_count @execution-count :payload [{:source "page" :data {:text/plain (str (:value execute-result))} :start 0}] :user_expressions {}}))

(defn send-out-message!
  [iopub-socket username parent-header session-id execute-result exception-stacktrace]
  (cond
    (:err execute-result)
    (send-message iopub-socket "error" username
                  {:execution_count @execution-count
                   :ename           (:ex execute-result)
                   :evalue          (:err execute-result)
                   :traceback       [exception-stacktrace]}
                  parent-header {} session-id)

    (= :chart (:type execute-result))
    (send-message iopub-socket "execute_result" username
                  {:execution_count @execution-count
                   :data {:image/png (:value execute-result)}
                   :metadata {:image/png {:width 500 :height 400}}}
                  parent-header {} session-id)

    :else
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


