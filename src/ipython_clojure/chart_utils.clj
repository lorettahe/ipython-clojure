(ns ipython-clojure.chart-utils
  (:require [clojure.data.codec.base64 :as b64])
  (:import [org.jfree.chart JFreeChart ChartUtilities]))

(defn get-chart-png-bytes
  [^JFreeChart chart]
  (ChartUtilities/encodeAsPNG (.createBufferedImage chart 500 400)))

(defn ipython-draw
  [^JFreeChart chart]
  (let [bytes (get-chart-png-bytes chart)]
    {:ipython-clojure.chart-utils/type :chart
     :ipython-clojure.chart-utils/data (String. (b64/encode bytes))}))

