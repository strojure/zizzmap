(ns strojure.vzmap.core
  (:require [strojure.vzmap.impl :as impl])
  (:import (clojure.lang IEditableCollection IFn IPersistentMap ITransientMap MapEquivalence)
           (java.util Iterator Map)
           (strojure.vzmap.impl BoxedValue)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;; TODO: Feature: Update delayed value without realizing it.

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
          (transient-map (.asTransient ^IEditableCollection m))))))

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
