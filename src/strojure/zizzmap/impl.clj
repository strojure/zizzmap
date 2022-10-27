(ns strojure.zizzmap.impl
  (:import (clojure.lang IDeref IEditableCollection IFn IMapEntry IMeta IObj
                         IPersistentMap IPersistentVector ITransientMap MapEntry
                         MapEquivalence)
           (java.util Iterator Map)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defprotocol InternalAccess
  (internal-map [_]
    "Returns underlying map for low-level manipulations."))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftype BoxedValue [d])

(defmacro boxed-value
  "Returns boxed delay for the `body`."
  [& body]
  `(BoxedValue. (delay ~@body)))

(defn deref-value
  "Returns value of the `BoxedValue` instance."
  [v]
  (.deref ^IDeref (.-d ^BoxedValue v)))

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
       (deref-value (.val e)))
     (getKey
       [_]
       (.key e))
     (getValue
       [_]
       (deref-value (.val e)))
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
       (cond-> (.valAt e i)
         (= i 1) (deref-value)))
     (valAt
       [_ i not-found]
       (cond-> (.valAt e i not-found)
         (= i 1) (deref-value)))
     (entryAt
       [_ i]
       (cond-> (.entryAt e i)
         (= i 1) (boxed-map-entry)))
     (cons
       [_ o]
       [(.key e), (deref-value (.val e)), o])
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
         (throw (IndexOutOfBoundsException.))))
     (seq
       [_]
       (lazy-seq (cons (.key e) (lazy-seq (cons (deref-value (.val e)) nil)))))
     (rseq
       [_]
       (rseq [(.key e) (deref-value (.val e))]))
     (nth
       [_ i]
       (case i
         0 (.key e)
         1 (deref-value (.val e))
         (throw (IndexOutOfBoundsException.))))
     (nth
       [_ i not-found]
       (case i
         0 (.key e)
         1 (deref-value (.val e))
         not-found))
     (pop
       [_]
       [(.key e)])
     (peek
       [_]
       (deref-value (.val e)))
     (empty
       [_]
       (.empty e))
     (equiv
       [_ o]
       (.equiv [(.key e) (deref-value (.val e))] o)))))

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
        (cond-> v (instance? BoxedValue v)
                  (deref-value)))))
  (invoke
    [_ k not-found]
    (let [v (.valAt m k NA)]
      (if (identical? NA v)
        not-found
        (cond-> v (instance? BoxedValue v)
                  (deref-value)))))
  IPersistentMap
  (valAt
    [_ k]
    (let [v (.valAt m k NA)]
      (if (identical? NA v)
        nil
        (cond-> v (instance? BoxedValue v)
                  (deref-value)))))
  (valAt
    [_ k not-found]
    (let [v (.valAt m k NA)]
      (if (identical? NA v)
        not-found
        (cond-> v (instance? BoxedValue v)
                  (deref-value)))))
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
  IEditableCollection
  (asTransient
    [_]
    (->TransientMap (.asTransient ^IEditableCollection m)))
  InternalAccess
  (internal-map
    [_]
    m)
  IObj
  (withMeta [_ meta*] (PersistentMap. nil (with-meta m meta*)))
  IMeta
  (meta [_] (.meta ^IMeta m)))

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
        (cond-> v (instance? BoxedValue v)
                  (deref-value)))))
  (valAt
    [_ k not-found]
    (let [v (.valAt m k NA)]
      (if (identical? NA v)
        not-found
        (cond-> v (instance? BoxedValue v)
                  (deref-value)))))
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

(defn assoc*
  "Returns persistent map with assoc’ed boxed value."
  [m k boxed-v]
  (-> m (cond-> (persistent? m) (internal-map))
      (assoc k boxed-v)
      (persistent-map)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
