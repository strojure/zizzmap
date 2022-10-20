(ns strojure.vzmap.core
  (:require [strojure.vzmap.impl :as impl])
  (:import (clojure.lang IDeref IFn IPersistentMap)
           (java.util Iterator)
           (strojure.vzmap.impl BoxedValue)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(let [none* (Object.)]

  (defn persistent-map
    [^IPersistentMap m]
    (reify
      IPersistentMap
      (valAt [_ k]
        (let [v (.valAt m k none*)]
          (if (identical? none* v)
            nil
            (if (instance? BoxedValue v)
              (impl/deref-value v)
              v))))
      (valAt [_ k not-found]
        (let [v (.valAt m k none*)]
          (if (identical? none* v)
            not-found
            (if (instance? BoxedValue v)
              (impl/deref-value v)
              v))))
      (assoc [_ k v] (persistent-map (.assoc m k v)))
      (assocEx [_ k v] (persistent-map (.assocEx m k v)))
      (without [_ k] (persistent-map (.without m k)))
      (containsKey [_ k] (.containsKey m k))
      (entryAt [_ k] (impl/map-entry (.entryAt m k)))
      (seq [_] (some->> (seq m) (map impl/map-entry)))
      (cons [_ o] (persistent-map (.cons m o)))
      (count [_] (.count m))
      (empty [_] (persistent-map (.empty m)))
      ;; TODO: equality?
      (equiv [_ o] (.equiv m o))
      (iterator [_] (let [it (.iterator m)]
                      (reify Iterator
                        (hasNext [_] (.hasNext it))
                        (next [_] (some-> (.next it) impl/map-entry)))))
      (forEach [_ a] (.forEach m a))
      (spliterator [_] (.spliterator m))
      #_#_#_IFn
              (invoke [_ k]
                      (let [v (.valAt m k none*)]
                        (if (identical? none* v)
                          nil
                          (if (instance? BoxedValue v)
                            (.deref ^IDeref (.-d ^BoxedValue v))
                            v))))
              (invoke [_ k not-found]
                      (let [v (.valAt m k none*)]
                        (if (identical? none* v)
                          not-found
                          (if (instance? BoxedValue v)
                            (.deref ^IDeref (.-d ^BoxedValue v))
                            v)))))))

(def my-class (class (persistent-map {})))

;; TODO: Check if it works in cljs
(defn my-class? [x] (instance? my-class x))

(def -m0 {:a 1
          :b 2})
(def -m (persistent-map {:a (impl/boxed-value (println "Init :a") 1)
                         :b 2}))

(comment
  (do (seq -m) nil)
  (seq -m)
  (MyMap. {:a (impl/boxed-value (println "Init :a") 1)
           :b 2})
  (persistent-map {:a (impl/boxed-value (println "Init :a") 1)
                   :b 2})
  (assoc -m :c (impl/boxed-value 3))
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
