(ns strojure.vzmap.impl-test
  (:require [clojure.test :as test :refer [deftest testing]]
            [strojure.vzmap.impl :as impl])
  (:import (clojure.lang IPersistentVector MapEntry)))

(set! *warn-on-reflection* true)

(declare thrown?)

#_(test/run-tests)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^:private -e (impl/boxed-map-entry :k (impl/boxed-value :x)))

(deftest boxed-map-entry-t
  (test/are [expr result] (= result expr)

    (key -e) #_= :k
    (val -e) #_= :x

    (count -e) #_= 2
    (.length ^IPersistentVector -e) #_= 2

    (nth -e 0) #_= :k
    (nth -e 1) #_= :x
    (nth -e 2 :not-found) #_= :not-found

    (.valAt ^IPersistentVector -e 0) #_= :k
    (.valAt ^IPersistentVector -e 1) #_= :x
    (.valAt ^IPersistentVector -e 2) #_= nil
    (.valAt ^IPersistentVector -e 2 :not-found) #_= :not-found

    -e #_= [:k :x]
    (let [[k v] -e] [k v]) #_= [:k :x]

    (conj -e :y) #_= [:k :x :y]
    (pop -e) #_= [:k]
    (peek -e) #_= :x

    (assoc -e 0 :kk) #_= [:kk :x]
    (assoc -e 1 :xx) #_= [:k :xx]
    (assoc -e 1 (impl/boxed-value :xx)) #_= [:k :xx]
    (assoc -e 2 :y) #_= [:k :x :y]

    (contains? -e 0) #_= true
    (contains? -e 1) #_= true
    (contains? -e 2) #_= false

    (.entryAt ^IPersistentVector -e 0) #_= [0 :k]
    (.entryAt ^IPersistentVector -e 1) #_= [1 :x]
    (.entryAt ^IPersistentVector -e 2) #_= nil
    (let [a (atom :pending)
          e (impl/boxed-map-entry :k (impl/boxed-value (reset! a :realized)
                                                       :x))
          e (.entryAt ^IPersistentVector e 1)]
      [e @a]) #_= [[1 :x] :pending]

    (.assocN ^IPersistentVector -e 0 :kk) #_= [:kk :x]
    (.assocN ^IPersistentVector -e 1 :xx) #_= [:k :xx]
    (.assocN ^IPersistentVector -e 1 (impl/boxed-value :xx)) #_= [:k :xx]
    (.assocN ^IPersistentVector -e 2 :y) #_= [:k :x :y]

    (empty -e) #_= (empty (MapEntry. :k :x))

    (realized? (seq -e)) #_= false
    (first (seq -e)) #_= :k
    (second (seq -e)) #_= :x

    (let [a (atom :pending)
          e (impl/boxed-map-entry :k (impl/boxed-value (reset! a :realized)
                                                       :x))]
      [(first e) @a]) #_= [:k :pending]

    (let [a (atom :pending)
          e (impl/boxed-map-entry :k (impl/boxed-value (reset! a :realized)
                                                       :x))]
      [(second e) @a]) #_= [:x :realized]

    (.equiv ^IPersistentVector -e -e) #_= true
    (.equiv ^IPersistentVector -e [:k :x]) #_= true
    (.equiv ^IPersistentVector -e (impl/boxed-map-entry :k (impl/boxed-value :x))) #_= true
    (.equiv ^IPersistentVector -e [:k :y]) #_= false

    (sequential? -e) #_= true

    (rseq -e) #_= '(:x :k)

    )

  (testing "Exceptional operations"
    (test/are [expr] expr

      (thrown? IndexOutOfBoundsException
               (nth -e 2))
      (thrown? IllegalArgumentException "Key must be integer"
               (assoc -e :x nil))

      )))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
