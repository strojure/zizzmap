(ns strojure.vzmap.core
  (:require [strojure.vzmap.impl :as impl])
  (:import (clojure.lang IEditableCollection IFn IPersistentMap ITransientMap MapEquivalence)
           (java.util Iterator Map)
           (strojure.vzmap.impl BoxedValue)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;; TODO: Feature: Update delayed value without realizing it.

;; TODO: Metadata support

(declare transient-map)

(let [NA (Object.)]

  (defn persistent-map
    "Returns `IPersistentMap` implementation for the map `m` which can contain
    delayed values."
    [^IPersistentMap m]
    (let [realized-delay (delay (into {} (map impl/map-entry) m))]
      (reify
        Map
        (size
          [_]
          (.count m))
        (get
          [this k]
          (.valAt this k))
        MapEquivalence
        IFn
        (invoke
          [_ k]
          (let [v (.valAt m k NA)]
            (if (identical? NA v)
              nil
              (cond-> v (instance? BoxedValue v)
                        (impl/deref-value)))))
        (invoke
          [_ k not-found]
          (let [v (.valAt m k NA)]
            (if (identical? NA v)
              not-found
              (cond-> v (instance? BoxedValue v)
                        (impl/deref-value)))))
        IPersistentMap
        (valAt
          [_ k]
          (let [v (.valAt m k NA)]
            (if (identical? NA v)
              nil
              (cond-> v (instance? BoxedValue v)
                        (impl/deref-value)))))
        (valAt
          [_ k not-found]
          (let [v (.valAt m k NA)]
            (if (identical? NA v)
              not-found
              (cond-> v (instance? BoxedValue v)
                        (impl/deref-value)))))
        (entryAt
          [_ k]
          (impl/map-entry (.entryAt m k)))
        (containsKey
          [_ k]
          (.containsKey m k))
        (assoc
          [_ k v]
          (persistent-map (.assoc m k v)))
        (assocEx
          [_ k v]
          (persistent-map (.assocEx m k v)))
        (cons
          [_ o]
          (persistent-map (.cons m o)))
        (without
          [_ k]
          (persistent-map (.without m k)))
        (empty
          [_]
          (persistent-map (.empty m)))
        (count
          [_]
          (.count m))
        (seq
          [_]
          (some->> (.seq m) (map impl/map-entry)))
        (equiv
          [_ o]
          (= @realized-delay o))
        (iterator
          [_]
          (let [it (.iterator m)]
            (reify Iterator
              (hasNext [_] (.hasNext it))
              (next [_] (some-> (.next it) impl/map-entry)))))
        IEditableCollection
        (asTransient
          [_]
          (transient-map (.asTransient ^IEditableCollection m)))
        impl/InternalAccess
        (internal-map
          [_]
          m))))

  (defn transient-map
    [^ITransientMap m]
    (reify
      ITransientMap
      (assoc
        [_ k v]
        (transient-map (.assoc m k v)))
      (conj
        [_ o]
        (transient-map (.conj m o)))
      (without
        [_ k]
        (transient-map (.without m k)))
      (persistent
        [_]
        (persistent-map (.persistent m)))
      (count
        [_]
        (.count m))
      (valAt
        [_ k]
        (let [v (.valAt m k NA)]
          (if (identical? NA v)
            nil
            (cond-> v (instance? BoxedValue v)
                      (impl/deref-value)))))
      (valAt
        [_ k not-found]
        (let [v (.valAt m k NA)]
          (if (identical? NA v)
            not-found
            (cond-> v (instance? BoxedValue v)
                      (impl/deref-value))))))))

(let [persistent-map-class (class (persistent-map {}))]
  ;; TODO: Check if it works in cljs
  (defn persistent?
    "True if `x` is an instance of persistent map implementation."
    [x]
    (instance? persistent-map-class x)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

#_(defmacro pending
    [& body]
    `(impl/boxed-value ~@body))

(defmacro pending-vals
  "Returns persistent map with every value wrapped in delayed computation.

  Fir example in

      (def my-map
        (pending-vals {:a (doto :x println)}))

  the code `(doto :x println)` will be evaluated only when value for `:a`
  requested, i.e. in `(get my-map :a)`."
  [m]
  (assert map? m)
  `(persistent-map ~(update-vals m (fn [v] `(impl/boxed-value ~v)))))

(comment
  (macroexpand-1 '(pending-vals {:a 1 :b 2}))
  (clojure.walk/macroexpand-all '(pending-vals {:a 1 :b 2}))
  )

(defn assoc-pending*
  [m k boxed-v]
  (-> m (cond-> (persistent? m) (impl/internal-map))
      (assoc k boxed-v)
      (persistent-map)))

(defmacro assoc-pending
  [m k v]
  `(assoc-pending* ~m ~k (impl/boxed-value ~v)))

(comment
  (macroexpand-1 '(assoc-pending {} :a 1))
  (clojure.walk/macroexpand-all '(assoc-pending {} :a 1))
  (assoc-pending {} :a (doto :x println))
  (-> {}
      (assoc-pending :a (doto :x println))
      (assoc-pending :b (doto :y println))
      (assoc-pending :c (doto :z println)))
  (assoc {} :a :x :b :y :c :z)
  (persistent-map {})
  (cond-> {} (persistent? {}) (impl/internal-map))
  (impl/internal-map (persistent-map {}))
  (persistent? (assoc-pending {} :a (doto :x println)))
  )

(defn merge*
  [m1 m2]
  (persistent-map (reduce conj
                          (cond-> (or m1 {}) (persistent? m1) (impl/internal-map))
                          (cond-> m2 (persistent? m2) (impl/internal-map)))))

(comment
  (merge {:a 1} {:b 2})
  (into {:a 1} {:b 2})
  (reduce conj {:a 1} {:b 2})
  (merge* {:a 1} {:b 2})
  (pending-vals {:a (doto :x println)})
  (merge* (pending-vals {:a (doto :x println)})
          (pending-vals {:b (doto :y println)}))
  (class (merge* (pending-vals {:a (doto :x println)})
                 (pending-vals {:b (doto :y println)})))
  (merge* {} (pending-vals {:a :x}))
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
