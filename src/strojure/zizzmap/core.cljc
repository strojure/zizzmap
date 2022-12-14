(ns strojure.zizzmap.core
  (:require [strojure.zizzmap.impl :as impl :include-macros true])
  #?(:cljs (:require-macros [strojure.zizzmap.core :refer [assoc*]])))

#?(:clj  (set! *warn-on-reflection* true)
   :cljs (set! *warn-on-infer* true))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defmacro delay*
  "Returns customized delay for the `body` to be used as zizz-map values when
  the map is constructed manually."
  [& body]
  `(impl/boxed-delay ~@body))

(defn convert-map
  "Converts any map to zizz map to allow access delayed values added explicitly."
  [m]
  (cond-> m (not (impl/persistent? m))
            (-> impl/underlying-map impl/persistent-map)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defmacro init
  "Returns persistent map with every not simple value expression wrapped with
  delayed evaluation.

  For example in

      (def my-map (init {:a (doto :x println)}))

  the expression `(doto :x println)` will be evaluated only when value for `:a`
  requested, i.e. in `(get my-map :a)`.

  The value expression is considered simple if it is not non-empty sequence. So
  in following case evaluation will not be delayed and initialization will be
  faster:

      (def my-map (init {:a 1}))
  "
  [m]
  (assert map? m)
  `(impl/persistent-map ~(update-vals m (fn [v] `(impl/boxed-delay* ~v)))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defmacro assoc*
  "Returns persistent map with delayed evaluations of the `expr` under the key
  `k`. Accepts multiple key/expr pairs. Does not delay evaluation of simple
  forms like constants and empty sequences."
  [m k expr & kvs]
  `(-> ^clojure.lang.Associative (impl/underlying-map ~m)
       (assoc ~k (impl/boxed-delay* ~expr))
       ~@(map (fn [[k v]] `(assoc ~k (impl/boxed-delay* ~v)))
              (partition 2 2 [`(throw (IllegalArgumentException. "Requires even amount of keys/values"))] kvs))
       (impl/persistent-map)))

(comment
  (macroexpand-1 '(assoc* {} :a 1))
  (macroexpand-1 '(assoc* {} :a 1 :b 2))
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn merge*
  "Given two maps with possibly delayed values returns merged persistent map."
  [m1 m2]
  (impl/persistent-map (reduce conj (impl/underlying-map m1) (impl/underlying-map m2))))

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
