(ns ipython-clojure.messaging.auto-complete
  (:require [ipython-clojure.messaging.message-proto :refer :all]
            [ipython-clojure.messaging.utils :refer [send-message]]
            [ipython-clojure.messaging.execute :refer [nrepl-session]]
            [clojure.tools.nrepl :as nrepl]))

(defmethod reply-to-message "complete_request"
  [message shell-socket iopub-socket nrepl-conn]
  (let [code (get-in message [:content :code])
        cursor-pos (get-in message [:content :cursor_pos])
        code-up-to-cursor (subs code 0 (inc cursor-pos))
        last-space (.lastIndexOf code-up-to-cursor " ")
        last-non-space-symbol (if (>= last-space 0) (subs code-up-to-cursor (inc last-space)))
        actual-symbol (clojure.string/replace last-non-space-symbol #"[(\[{,}\])]+" "")
        code-count (count actual-symbol)
        resp (-> (nrepl/client nrepl-conn 1000)
                 (nrepl/message {:op "complete" :symbol actual-symbol :context "" :ns "user"}))
        result (->> resp first :completions (map :candidate))]
    (println resp)
    (println "cursor_start:" (- cursor-pos code-count))
    (send-message
      shell-socket
      "complete_reply"
      (get-in message [:header :username])
      {:matches result
       :matched_text result
       :cursor_start (- cursor-pos code-count)
       :cursor_end cursor-pos
       :metadata {}
       :status "ok"}
      (:header message)
      {}
      (get-in message [:header :session]))))

