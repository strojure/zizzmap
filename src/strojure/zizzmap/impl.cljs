(ns strojure.zizzmap.impl)

(set! *warn-on-infer* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defprotocol InternalAccess
  (internal-map [_]
    "Returns underlying map for low-level manipulations."))

(extend-protocol InternalAccess
  nil,,,,,,,,,,,,,,, (internal-map [_] {})
  ObjMap,,,,,,,,,,,, (internal-map [this] this)
  PersistentArrayMap (internal-map [this] this)
  PersistentHashMap, (internal-map [this] this)
  ITransientMap,,,,, (internal-map [this] this))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftype BoxedValue [d])

(defmacro boxed-value
  "Returns boxed delay for the `body`."
  [& body]
  `(BoxedValue. (delay ~@body)))

(defn deref-value
  "Returns value of the `BoxedValue` instance."
  [v]
  (-deref (.-d ^BoxedValue v)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn boxed-map-entry
  "Returns map entry with delayed value which is deref’ed when accessed."
  ([k, boxed-v]
   (boxed-map-entry (->MapEntry k boxed-v nil)))
  ([e]
   (reify
     IMapEntry
     (-key
       [_]
       (-key e))
     (-val
       [_]
       (deref-value (-val e)))
     ICounted
     (-count
       [_]
       2)
     IAssociative
     (-assoc
       [this i o]
       (if (int? i)
         (-assoc-n this i o)
         (throw (js/Error. "Key must be integer"))))
     (-contains-key?
       [_ i]
       (-contains-key? e i))
     ILookup
     (-lookup
       [_ i]
       (cond-> (-lookup e i)
         (= i 1) (deref-value)))
     (-lookup
       [_ i not-found]
       (cond-> (-lookup e i not-found)
         (= i 1) (deref-value)))
     IFind
     (-find
       [_ i]
       (cond-> (-find e i)
         (= i 1) (boxed-map-entry)))
     ICollection
     (-conj
       [_ o]
       [(-key e), (deref-value (-val e)), o])
     IVector
     (-assoc-n
       [this i o]
       (case i
         0 (boxed-map-entry o (-val e))
         1 (if (instance? BoxedValue o)
             (boxed-map-entry (-key e) o)
             [(-key e) o])
         2 (-conj this o)
         (-assoc-n e i o)))
     ISequential
     ISeqable
     (-seq
       [_]
       (lazy-seq (cons (-key e) (lazy-seq (cons (deref-value (-val e)) nil)))))
     IReversible
     (-rseq
       [_]
       (rseq [(-key e) (deref-value (-val e))]))
     IIndexed
     (-nth
       [_ i]
       (case i
         0 (-key e)
         1 (deref-value (-val e))
         (-nth e i)))
     (-nth
       [_ i not-found]
       (case i
         0 (-key e)
         1 (deref-value (-val e))
         not-found))
     IStack
     (-pop
       [_]
       [(-key e)])
     (-peek
       [_]
       (deref-value (-val e)))
     IEmptyableCollection
     (-empty
       [_]
       (-empty e))
     IEquiv
     (-equiv
       [_ o]
       (-equiv [(-key e) (deref-value (-val e))] o)))))

(defn map-entry
  "Returns map entry, the standard one or the implementation for boxed value."
  [e]
  (if (instance? BoxedValue (-val e))
    (boxed-map-entry e)
    e))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^:private NA (js/Object.))

(declare ->TransientMap)

(deftype PersistentMap [^:mutable realized!, m]
  IFn
  (-invoke
    [_ k]
    (let [v (-lookup m k NA)]
      (if (identical? NA v)
        nil
        (cond-> v (instance? BoxedValue v)
                  (deref-value)))))
  (-invoke
    [_ k not-found]
    (let [v (-lookup m k NA)]
      (if (identical? NA v)
        not-found
        (cond-> v (instance? BoxedValue v)
                  (deref-value)))))
  ILookup
  (-lookup
    [_ k]
    (let [v (-lookup m k NA)]
      (if (identical? NA v)
        nil
        (cond-> v (instance? BoxedValue v)
                  (deref-value)))))
  (-lookup
    [_ k not-found]
    (let [v (-lookup m k NA)]
      (if (identical? NA v)
        not-found
        (cond-> v (instance? BoxedValue v)
                  (deref-value)))))
  IFind
  (-find
    [_ k]
    (map-entry (-find m k)))
  IAssociative
  (-contains-key?
    [_ k]
    (-contains-key? m k))
  (-assoc
    [_ k v]
    (PersistentMap. nil (-assoc m k v)))
  ICollection
  (-conj
    [_ o]
    (PersistentMap. nil (-conj m o)))
  IMap
  (-dissoc
    [_ k]
    (PersistentMap. nil (-dissoc m k)))
  IEmptyableCollection
  (-empty
    [_]
    (PersistentMap. nil (-empty m)))
  ICounted
  (-count
    [_]
    (-count m))
  ISeqable
  (-seq
    [_]
    (some->> (-seq m) (map map-entry)))
  IEquiv
  (-equiv
    [_ o]
    (= o (or realized! (set! realized! (into {} (map map-entry) m)))))
  IIterable
  (-iterator
    [_]
    (let [it (-iterator m)]
      (reify Object
        (hasNext [_] (.hasNext ^HashMapIter it))
        (next [_] (some-> (.next ^HashMapIter it) map-entry)))))
  IEditableCollection
  (-as-transient
    [_]
    (->TransientMap (-as-transient m)))
  InternalAccess
  (internal-map
    [_]
    m)
  IWithMeta
  (-with-meta
    [_ meta*]
    (PersistentMap. nil (with-meta m meta*)))
  IMeta
  (-meta
    [_]
    (-meta m)))

(defn persistent-map
  "Returns `IPersistentMap` implementation for the map `m` which can contain
  delayed values."
  {:inline (fn [m] `(PersistentMap. nil ~m))}
  [m]
  (PersistentMap. nil m))

(deftype TransientMap [m]
  ITransientAssociative
  (-assoc!
    [_ k v]
    (TransientMap. (-assoc! m k v)))
  ITransientCollection
  (-conj!
    [_ o]
    (TransientMap. (-conj! m o)))
  (-persistent!
    [_]
    (persistent-map (-persistent! m)))
  ITransientMap
  (-dissoc!
    [_ k]
    (TransientMap. (-dissoc! m k)))
  ICounted
  (-count
    [_]
    (-count m))
  ILookup
  (-lookup
    [_ k]
    (let [v (-lookup m k NA)]
      (if (identical? NA v)
        nil
        (cond-> v (instance? BoxedValue v)
                  (deref-value)))))
  (-lookup
    [_ k not-found]
    (let [v (-lookup m k NA)]
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
  (-> (internal-map m)
      (assoc k boxed-v)
      (persistent-map)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
