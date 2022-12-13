(ns strojure.zizzmap.impl
  (:import (clojure.lang Delay IDeref IEditableCollection IFn IKVReduce IMapEntry IMeta IObj
                         IPersistentMap IPersistentVector ITransientMap MapEntry
                         MapEquivalence RT)
           (java.util Iterator Map)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defprotocol InternalAccess
  (internal-map [_]
    "Returns underlying map for low-level manipulations."))

(extend-protocol InternalAccess
  nil,,,,,,,,,,, (internal-map [_] {})
  IPersistentMap (internal-map [this] this)
  ITransientMap, (internal-map [this] this))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftype BoxedValue [^Delay d]
  IDeref
  (deref [_] (.deref d)))

(defmacro boxed-value
  "Returns boxed delay for the `body`."
  [& body]
  `(BoxedValue. (delay ~@body)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn boxed-map-entry
  "Returns map entry with delayed value which is deref’ed when accessed."
  ([k, boxed-v]
   (boxed-map-entry (MapEntry. k boxed-v)))
  ([^MapEntry e]
   (reify
     IMapEntry
     (key
       [_]
       (.key e))
     (val
       [_]
       (.deref ^IDeref (.val e)))
     (getKey
       [_]
       (.key e))
     (getValue
       [_]
       (.deref ^IDeref (.val e)))
     IPersistentVector
     (count
       [_]
       2)
     (length
       [_]
       2)
     (containsKey
       [_ i]
       (.containsKey e i))
     (valAt
       [_ i]
       (cond-> ^IDeref (.valAt e i)
         (= i 1) (.deref)))
     (valAt
       [_ i not-found]
       (cond-> ^IDeref (.valAt e i not-found)
         (= i 1) (.deref)))
     (entryAt
       [_ i]
       (cond-> (.entryAt e i)
         (= i 1) (boxed-map-entry)))
     (cons
       [_ o]
       [(.key e), (.deref ^IDeref (.val e)), o])
     (assoc
       [this i o]
       (if (int? i)
         (.assocN this i o)
         (throw (IllegalArgumentException. "Key must be integer"))))
     (assocN
       [this i o]
       (case i
         0 (boxed-map-entry o (.val e))
         1 (if (instance? BoxedValue o)
             (boxed-map-entry (.key e) o)
             [(.key e) o])
         2 (.cons this o)
         (.assocN e i o)))
     (seq
       [_]
       (lazy-seq (cons (.key e) (lazy-seq (cons (.deref ^IDeref (.val e)) nil)))))
     (rseq
       [_]
       (rseq [(.key e) (.deref ^IDeref (.val e))]))
     (nth
       [_ i]
       (cond-> ^IDeref (.nth e i)
         (= i 1) (.deref)))
     (nth
       [_ i not-found]
       (cond-> ^IDeref (.nth e i not-found)
         (= i 1) (.deref)))
     (pop
       [_]
       [(.key e)])
     (peek
       [_]
       (.deref ^IDeref (.val e)))
     (empty
       [_]
       (.empty e))
     (equiv
       [_ o]
       (.equiv [(.key e) (.deref ^IDeref (.val e))] o)))))

(defn map-entry
  "Returns map entry, the standard one or the implementation for boxed value."
  [^IMapEntry e]
  (if (instance? BoxedValue (.val e))
    (boxed-map-entry e)
    e))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^:private NA (Object.))

(declare ->TransientMap)

(deftype PersistentMap [^:unsynchronized-mutable realized!
                        ^IPersistentMap m]
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
        (cond-> ^IDeref v (instance? BoxedValue v)
                          (.deref)))))
  (invoke
    [_ k not-found]
    (let [v (.valAt m k NA)]
      (if (identical? NA v)
        not-found
        (cond-> ^IDeref v (instance? BoxedValue v)
                          (.deref)))))
  IPersistentMap
  (valAt
    [_ k]
    (let [v (.valAt m k NA)]
      (if (identical? NA v)
        nil
        (cond-> ^IDeref v (instance? BoxedValue v)
                          (.deref)))))
  (valAt
    [_ k not-found]
    (let [v (.valAt m k NA)]
      (if (identical? NA v)
        not-found
        (cond-> ^IDeref v (instance? BoxedValue v)
                          (.deref)))))
  (entryAt
    [_ k]
    (map-entry (.entryAt m k)))
  (containsKey
    [_ k]
    (.containsKey m k))
  (assoc
    [_ k v]
    (PersistentMap. nil (.assoc m k v)))
  (assocEx
    [_ k v]
    (PersistentMap. nil (.assocEx m k v)))
  (cons
    [_ o]
    (PersistentMap. nil (.cons m o)))
  (without
    [_ k]
    (PersistentMap. nil (.without m k)))
  (empty
    [_]
    (PersistentMap. nil (.empty m)))
  (count
    [_]
    (.count m))
  (seq
    [_]
    (some->> (.seq m) (map map-entry)))
  (equiv
    [_ o]
    (= o (or realized! (set! realized! (into {} (map map-entry) m)))))
  (iterator
    [_]
    (let [it (.iterator m)]
      (reify Iterator
        (hasNext [_] (.hasNext it))
        (next [_] (some-> (.next it) map-entry)))))
  IKVReduce
  (kvreduce
    [_ f init]
    (.kvreduce ^IKVReduce m
               (fn [x k v] (f x k (cond-> ^IDeref v (instance? BoxedValue v)
                                                    (.deref))))
               init))
  IEditableCollection
  (asTransient
    [_]
    (->TransientMap (.asTransient ^IEditableCollection m)))
  InternalAccess
  (internal-map
    [_]
    m)
  IObj
  (withMeta
    [_ meta*]
    (PersistentMap. nil (with-meta m meta*)))
  IMeta
  (meta
    [_]
    (.meta ^IMeta m))
  Object
  (toString
    [this]
    (RT/printString this)))

(defn persistent-map
  "Returns `IPersistentMap` implementation for the map `m` which can contain
  delayed values."
  {:inline (fn [m] `(PersistentMap. nil ~m))}
  [m]
  (PersistentMap. nil m))

(deftype TransientMap [^ITransientMap m]
  ITransientMap
  (assoc
    [_ k v]
    (TransientMap. (.assoc m k v)))
  (conj
    [_ o]
    (TransientMap. (.conj m o)))
  (without
    [_ k]
    (TransientMap. (.without m k)))
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
        (cond-> ^IDeref v (instance? BoxedValue v)
                          (.deref)))))
  (valAt
    [_ k not-found]
    (let [v (.valAt m k NA)]
      (if (identical? NA v)
        not-found
        (cond-> ^IDeref v (instance? BoxedValue v)
                          (.deref)))))
  InternalAccess
  (internal-map
    [_]
    m))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn persistent?
  "True if `x` is an instance of persistent map implementation."
  {:inline (fn [x] `(instance? PersistentMap ~x))}
  [x]
  (instance? PersistentMap x))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
