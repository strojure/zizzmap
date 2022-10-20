(ns strojure.vzmap.core-test
  (:require [clojure.test :as test :refer [deftest testing]])
  (:require [strojure.vzmap.core :as map]
            [strojure.vzmap.impl :as impl])
  (:import (java.util Map)))

(set! *warn-on-reflection* true)

(declare thrown?)

#_(test/run-tests)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^:private -m
  (map/persistent-map {:a (impl/boxed-value :x)
                       :b :y}))

(deftest persistent-map-t
  (test/are [expr result] (= result expr)

    (get -m :a) #_= :x
    (get -m :b) #_= :y
    (get -m :c) #_= nil

    (get -m :a :not-found) #_= :x
    (get -m :b :not-found) #_= :y
    (get -m :c :not-found) #_= :not-found

    (:a -m) #_= :x
    (:b -m) #_= :y
    (:c -m) #_= nil

    (-m :a) #_= :x
    (-m :b) #_= :y
    (-m :c) #_= nil

    (instance? Map -m) #_= true

    (= -m -m) #_= true
    (= -m {:a :x :b :y}) #_= true
    (= {:a :x :b :y} -m) #_= true
    (= (map/persistent-map {:a (impl/boxed-value :x)})
       (map/persistent-map {:a (impl/boxed-value :x)})) #_= true

    (assoc -m :a :xx) #_= {:a :xx :b :y}
    (assoc -m :b :yy) #_= {:a :x :b :yy}
    (assoc -m :c :zz) #_= {:a :x :b :y :c :zz}

    (dissoc -m :a) #_= {:b :y}
    (dissoc -m :b) #_= {:a :x}
    (dissoc -m :c) #_= {:a :x :b :y}

    (select-keys -m [:a :b]) #_= {:a :x :b :y}
    (select-keys -m [:a]) #_= {:a :x}
    (select-keys -m [:b]) #_= {:b :y}

    (seq -m) #_= '([:a :x] [:b :y])

    (into {} -m) #_= {:a :x :b :y}
    (into -m {}) #_= {:a :x :b :y}
    (into {:c :z} -m) #_= {:a :x :b :y :c :z}
    (into -m {:c :z}) #_= {:a :x :b :y :c :z}
    (map/persistent? (into {:c :z} -m)) #_= false
    (map/persistent? (into -m {:c :z})) #_= true

    (counted? -m) #_= true
    (count -m) #_= 2

    (set (keys -m)) #_= #{:a :b}
    (set (vals -m)) #_= #{:x :y}

    (conj -m [:c :z]) #_= {:a :x, :b :y, :c :z}
    (conj -m [:c (impl/boxed-value :z)]) #_= {:a :x, :b :y, :c :z}

    (empty -m) #_= {}
    (map/persistent? (empty -m)) #_= true

    (find -m :a) #_= [:a :x]

    )

  (testing "Value laziness"
    (test/are [expr result] (= result expr)

      (let [a (atom :pending) m (map/persistent-map {:a (impl/boxed-value
                                                          (reset! a :realized)
                                                          :x)})
            before @a
            x (get m :a)
            after @a]
        [before x after]) #_= [:pending :x :realized]

      (let [a (atom :pending) m (map/persistent-map {:a (impl/boxed-value
                                                          (reset! a :realized)
                                                          :x)})
            m (assoc m :a :xx)]
        [@a m]) #_= [:pending {:a :xx}]

      (let [a (atom :pending) m (map/persistent-map {:a (impl/boxed-value
                                                          (reset! a :realized)
                                                          :x)})
            m (assoc m :b :yy)]
        [@a m]) #_= [:pending {:a :x :b :yy}]

      (let [a (atom :pending) m (map/persistent-map {:a (impl/boxed-value
                                                          (reset! a :realized)
                                                          :x)
                                                     :b :y})
            m (dissoc m :a)]
        [@a m]) #_= [:pending {:b :y}]

      (let [a (atom :pending) m (map/persistent-map {:a (impl/boxed-value
                                                          (reset! a :realized)
                                                          :x)
                                                     :b :y})
            m (dissoc m :b)]
        [@a m]) #_= [:pending {:a :x}]

      (let [a (atom :pending) m (map/persistent-map {:a (impl/boxed-value
                                                          (reset! a :realized)
                                                          :x)
                                                     :b :y})
            m (select-keys m [:a :b])]
        [@a m]) #_= [:realized {:a :x :b :y}]

      (let [a (atom :pending) m (map/persistent-map {:a (impl/boxed-value
                                                          (reset! a :realized)
                                                          :x)
                                                     :b :y})
            m (select-keys m [:b])]
        [@a m]) #_= [:pending {:b :y}]

      (let [a (atom :pending) m (map/persistent-map {:a (impl/boxed-value
                                                          (reset! a :realized)
                                                          :x)
                                                     :b :y})
            _ (doall (seq m))]
        [@a]) #_= [:pending]

      (let [a (atom :pending) m (map/persistent-map {:a (impl/boxed-value
                                                          (reset! a :realized)
                                                          :x)
                                                     :b :y})
            m (into m {:c :z})]
        [@a m]) #_= [:pending {:a :x, :b :y, :c :z}]

      (let [a (atom :pending) m (map/persistent-map {:a (impl/boxed-value
                                                          (reset! a :realized)
                                                          :x)
                                                     :b :y})
            m (into {:c :z} m)]
        [@a m]) #_= [:realized {:a :x, :b :y, :c :z}]

      (let [a (atom :pending) m (map/persistent-map {:a (impl/boxed-value
                                                          (reset! a :realized)
                                                          :x)
                                                     :b :y})
            m (conj m [:c :z])]
        [@a m]) #_= [:pending {:a :x, :b :y, :c :z}]

      (let [a (atom :pending) m (map/persistent-map {:a (impl/boxed-value
                                                          (reset! a :realized)
                                                          :x)
                                                     :b :y})
            k (key (find m :a))]
        [@a k]) #_= [:pending :a]

      (let [a (atom :pending) m (map/persistent-map {:a (impl/boxed-value
                                                          (reset! a :realized)
                                                          :x)
                                                     :b :y})
            v (val (find m :a))]
        [@a v]) #_= [:realized :x]

      ))
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
