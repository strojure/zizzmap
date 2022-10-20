(ns strojure.vzmap.impl
  (:import (clojure.lang IDeref IMapEntry IPersistentVector MapEntry)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defrecord BoxedValue [d])

(defmacro boxed-value
  "Returns boxed delay for the `body`."
  [& body]
  `(->BoxedValue (delay ~@body)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn deref-value
  "Returns value of the `BoxedValue` instance."
  [v]
  (.deref ^IDeref (.-d ^BoxedValue v)))

;; Map entry with delayed value which is dereferred when accessed.

(deftype BoxedEntry [^MapEntry e]

  IMapEntry
  (key [_] (.key e))
  (val [_] (deref-value (.val e)))
  (getKey [_] (.key e))
  (getValue [_] (deref-value (.val e)))

  IPersistentVector
  (count [_] 2)
  (length [_] 2)
  (containsKey [_ i] (.containsKey e i))
  (valAt [_ i]
    (cond-> (.valAt e i)
      (= i 1) (deref-value)))
  (valAt [_ i not-found]
    (cond-> (.valAt e i not-found)
      (= i 1) (deref-value)))
  (entryAt [_ i]
    (cond-> (.entryAt e i)
      (= i 1) (BoxedEntry.)))
  (cons [_ o]
    [(.key e)
     (deref-value (.val e))
     o])
  (assoc [this i o]
    (if (int? i)
      (.assocN this i o)
      (throw (IllegalArgumentException. "Key must be integer"))))
  (assocN [this i o]
    (case i
      0 (BoxedEntry. (MapEntry. o (.val e)))
      1 (if (instance? BoxedValue o)
          (BoxedEntry. (MapEntry. (.key e) o))
          [(.key e) o])
      2 (.cons this o)
      (throw (IndexOutOfBoundsException.))))
  (seq [_]
    (lazy-seq (cons (.key e) (lazy-seq (cons (deref-value (.val e)) nil)))))
  (rseq [_] (rseq [(.key e) (deref-value (.val e))]))
  (nth [_ i]
    (case i
      0 (.key e)
      1 (deref-value (.val e))
      (throw (IndexOutOfBoundsException.))))
  (nth [_ i not-found]
    (case i
      0 (.key e)
      1 (deref-value (.val e))
      not-found))
  (pop [_] [(.key e)])
  (peek [_] (deref-value (.val e)))
  (empty [_] (.empty e))
  (equiv [_ o] (.equiv [(.key e) (deref-value (.val e))] o)))

(defn map-entry
  "Returns map entry, the standard one or the implementation for boxed value."
  [^IMapEntry e]
  (if (instance? BoxedValue (.val e))
    (BoxedEntry. e)
    e))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
