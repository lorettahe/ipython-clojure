(ns ipython-clojure.messaging.utils
  (:require [zeromq.zmq :as zmq]
            [cheshire.core :as cheshire]
            [clj-time.core :as time]
            [clj-time.format :as time-format])
  (:import [java.util UUID]))

(defn now
  []
  "Returns current ISO 8601 compliant date."
  (let [current-date-time (time/to-time-zone (time/now) (time/default-time-zone))]
    (time-format/unparse
      (time-format/with-zone (time-format/formatters :date-time-no-ms)
                             (.getZone current-date-time))
      current-date-time)))

(defn uuid
  []
  (str (UUID/randomUUID)))

(defn read-blob [socket]
  (let [part (zmq/receive socket)
        blob (String. part "UTF-8")]
    blob))

(defn read-until-delimiter [socket]
  (let [_ (zmq/receive socket)
        preamble (doall (drop-last
                          (take-while (comp not #(= "<IDS|MSG>" %))
                                      (repeatedly #(read-blob socket)))))]
    preamble))

(defn address [config service]
  (str (:transport config) "://" (:ip config) ":" (service config)))

(defn read-message [socket]
  {:uuid (read-until-delimiter socket)
   :signature (read-blob socket)
   :header (cheshire/parse-string (read-blob socket) keyword)
   :parent-header (cheshire/parse-string (read-blob socket) keyword)
   :metadata (cheshire/parse-string (read-blob socket) keyword)
   :content (cheshire/parse-string (read-blob socket) keyword)})

(defn new-header [username msg-type session-id]
  {:date (now)
   :msg_id (uuid)
   :username username
   :session session-id
   :msg_type msg-type
   :version "5.0"})

(defn send-message-piece [socket msg]
  (zmq/send socket (.getBytes msg) zmq/send-more))

(defn finish-message [socket msg]
  (zmq/send socket (.getBytes msg)))

(defn send-message [socket msg-type username content parent-header metadata session-id]
  (let [header (cheshire/generate-string (new-header username msg-type session-id))
        parent-header (cheshire/generate-string parent-header)
        metadata (cheshire/generate-string metadata)
        content (cheshire/generate-string content)]
    (send-message-piece socket session-id)
    (send-message-piece socket "<IDS|MSG>")
    (send-message-piece socket "")
    (send-message-piece socket header)
    (send-message-piece socket parent-header)
    (send-message-piece socket metadata)
    (finish-message socket content)))

