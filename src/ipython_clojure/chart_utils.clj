(ns ipython-clojure.chart-utils
  (:import [org.jfree.chart JFreeChart ChartUtilities]
           (java.util Base64)))

(defn get-chart-png-bytes
  [^JFreeChart chart]
  (ChartUtilities/encodeAsPNG (.createBufferedImage chart 500 400)))

(defn ipython-draw
  [^JFreeChart chart]
  (let [bytes (get-chart-png-bytes chart)]
    {:ipython-clojure.chart-utlls/type :chart
     :ipython-clojure.chart-utils/data (String. (.encode (Base64/getEncoder) bytes))}))

