(ns ipython-clojure.messaging.repl-utils)

(defn extract-actual-symbol-from-code
  [code cursor-pos]
  (let [code-up-to-cursor (subs code 0 (inc cursor-pos))
        last-space (.lastIndexOf code-up-to-cursor " ")
        last-non-space-symbol (if (>= last-space 0) (subs code-up-to-cursor (inc last-space)) code-up-to-cursor)
        actual-symbol (clojure.string/replace last-non-space-symbol #"[(\[{,}\])]+" "")]
    actual-symbol))
