(ns strojure.zizzmap.core
  (:require [strojure.zizzmap.impl :as impl]))

(set! *warn-on-reflection* true)

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
  (impl/persistent-map (reduce conj (impl/internal-map m1) (impl/internal-map m2))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn update*
  "Same as `clojure.core/update` but with delayed application of the function `f`."
  ([m k f]
   (assoc* m k (f (get m k))))
  ([m k f x]
   (assoc* m k (f (get m k) x)))
  ([m k f x y]
   (assoc* m k (f (get m k) x y)))
  ([m k f x y z]
   (assoc* m k (f (get m k) x y z)))
  ([m k f x y z & more]
   (assoc* m k (apply f (get m k) x y z more))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
