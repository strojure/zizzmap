(ns strojure.zizzmap.core
  (:require [strojure.zizzmap.impl :as impl]))

(set! *warn-on-reflection* true)

;; TODO: Feature: Update delayed value without realizing it.

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defmacro init
  "Returns persistent map with every value wrapped with delayed evaluation.

  Fir example in

      (def my-map (init {:a (doto :x println)}))

  the expression `(doto :x println)` will be evaluated only when value for `:a`
  requested, i.e. in `(get my-map :a)`."
  [m]
  (assert map? m)
  `(impl/persistent-map ~(update-vals m (fn [v] `(impl/boxed-value ~v)))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defmacro assoc*
  "Returns persistent map with delayed evaluations of the `expr` under the key `k`."
  [m k expr]
  `(impl/assoc* ~m ~k (impl/boxed-value ~expr)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn merge*
  "Given two maps with possibly delayed values returns merged persistent map."
  [m1 m2]
  (-> (reduce conj
              (cond-> (or m1 {}) (impl/persistent? m1) (impl/internal-map))
              (cond-> m2 (impl/persistent? m2) (impl/internal-map)))
      (impl/persistent-map)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
