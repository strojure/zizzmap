(ns strojure.vzmap.core
  (:import (clojure.lang IDeref IFn ILookup IMapEntry IPersistentMap IPersistentVector Indexed MapEntry)
           (java.util Iterator Map$Entry)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defrecord Delayed [d])

(defmacro delayed
  [& body]
  `(->Delayed (delay ~@body)))

#_(defrecord Entry [k, ^Delayed v]
    IMapEntry
    (key [_] k)
    (val [_] (.deref ^IDeref (.-d v)))
    (getKey [_] k)
    (getValue [_] (.deref ^IDeref (.-d v))))

(deftype Entry [^MapEntry e]
  IMapEntry
  (key [_] (.key e))
  (val [_] (.deref ^IDeref (.-d ^Delayed (.val e))))
  (getKey [_] (.key e))
  (getValue [_] (.deref ^IDeref (.-d ^Delayed (.val e))))
  IPersistentVector
  (length [_] (.length e))
  (assocN [_ i o] (.assocN e i o))
  (cons [_ o] (.cons e o))
  (seq [_] (->> (cons (.deref ^IDeref (.-d ^Delayed (.val e))) nil)
                (lazy-seq)
                (cons (.key e))
                (lazy-seq)))
  #_(seq [_] (->> (seq e)
                  (map-indexed (fn [i v] (if (= i 1)
                                           (.deref ^IDeref (.-d ^Delayed v))
                                           v)))))
  (nth [_ i] (case i 0 (.key e)
                     1 (.deref ^IDeref (.-d ^Delayed (.val e)))
                     (IndexOutOfBoundsException.)))
  (nth [_ i nf] (case i 0 (.key e)
                        1 (.deref ^IDeref (.-d ^Delayed (.val e)))
                        nf)))

(defn map-entry
  [^IMapEntry e]
  (if (instance? Delayed (.val e))
    (Entry. e)
    e))

#_(defn map-entry
    [^IMapEntry e]
    (if (instance? Delayed (.val e))
      (MapEntry/create (.key e) (.deref ^IDeref (.-d ^Delayed (.val e))))
      e))

(let [none* (Object.)]

  (defn persistent-map
    [^IPersistentMap m]
    (reify
      IPersistentMap
      (valAt [_ k]
        (let [v (.valAt m k none*)]
          (if (identical? none* v)
            nil
            (if (instance? Delayed v)
              (.deref ^IDeref (.-d ^Delayed v))
              v))))
      (valAt [_ k not-found]
        (let [v (.valAt m k none*)]
          (if (identical? none* v)
            not-found
            (if (instance? Delayed v)
              (.deref ^IDeref (.-d ^Delayed v))
              v))))
      (assoc [_ k v] (persistent-map (.assoc m k v)))
      (assocEx [_ k v] (persistent-map (.assocEx m k v)))
      (without [_ k] (persistent-map (.without m k)))
      (containsKey [_ k] (.containsKey m k))
      (entryAt [_ k] (map-entry (.entryAt m k)))
      (seq [_] (some->> (seq m) (map map-entry)))
      (cons [_ o] (persistent-map (.cons m o)))
      (count [_] (.count m))
      (empty [_] (persistent-map (.empty m)))
      ;; TODO: equality?
      (equiv [_ o] (.equiv m o))
      (iterator [_] (let [it (.iterator m)]
                      (reify Iterator
                        (hasNext [_] (.hasNext it))
                        (next [_] (some-> (.next it) map-entry)))))
      (forEach [_ a] (.forEach m a))
      (spliterator [_] (.spliterator m))
      #_#_#_IFn
              (invoke [_ k]
                      (let [v (.valAt m k none*)]
                        (if (identical? none* v)
                          nil
                          (if (instance? Delayed v)
                            (.deref ^IDeref (.-d ^Delayed v))
                            v))))
              (invoke [_ k not-found]
                      (let [v (.valAt m k none*)]
                        (if (identical? none* v)
                          not-found
                          (if (instance? Delayed v)
                            (.deref ^IDeref (.-d ^Delayed v))
                            v)))))))

(def my-class (class (persistent-map {})))

;; TODO: Check if it works in cljs
(defn my-class? [x] (instance? my-class x))

(def -m0 {:a 1
          :b 2})
(def -m (persistent-map {:a (delayed (println "Init :a") 1)
                         :b 2}))

(comment
  (do (seq -m) nil)
  (seq -m)
  (MyMap. {:a (delayed (println "Init :a") 1)
           :b 2})
  (persistent-map {:a (delayed (println "Init :a") 1)
                   :b 2})
  (assoc -m :c (delayed 3))
  (assoc -m :c 3)
  (assoc -m0 :c 3)
  (dissoc -m :b)
  (my-class? -m)
  (my-class? -m0)
  (select-keys -m [:a])
  (into {} -m)
  (class (into {} -m))
  (class -m)
  (get -m :a)
  (get -m :b)
  (seq (first -m))
  (first (seq (first -m)))
  (do (seq (first -m)) nil)
  (let [[k v] (first -m)]
    [k v])
  (counted? -m)
  (count -m)
  (keys -m)
  (conj -m [:c 3])
  (do (conj -m [:c 3]) nil)
  (empty -m)
  (= -m0 -m)
  (= -m -m0)
  (= (delay 1) (delay 1))
  (:a -m)
  (:b -m)
  (:c -m)
  (-m :a)
  (-m :b)
  (-m :x :x)
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
