(ns strojure.zizzmap.core-test
  (:require [clojure.test :as test :refer [deftest]]
            [strojure.zizzmap.core :as map]
            [strojure.zizzmap.impl :as impl]))

(set! *warn-on-reflection* true)

(declare thrown?)

#_(test/run-tests)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest init-t
  (test/are [expr result] (= result expr)

    (let [a (atom :pending)
          m (map/init {:a (do (reset! a :realized)
                              :x)})]
      {:persistent (impl/persistent? m)
       :atom @a
       :value (:a m)
       :equal (= m {:a :x})})
    {:persistent true, :atom :pending, :value :x, :equal true}

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest assoc*-t
  (test/are [expr result] (= result expr)

    (let [a (atom :pending)
          m (map/assoc* {} :a (do (reset! a :realized)
                                  :x))]
      {:persistent (impl/persistent? m)
       :atom @a
       :value (:a m)
       :equal (= m {:a :x})})
    {:persistent true, :atom :pending, :value :x, :equal true}

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest merge*-t
  (test/are [expr result] (= result expr)

    (let [a (atom :pending)
          m (map/merge* {:b :y} (map/init {:a (do (reset! a :realized)
                                                  :x)}))]
      {:persistent (impl/persistent? m)
       :atom @a
       :value (:a m)
       :equal (= m {:a :x :b :y})})
    {:persistent true, :atom :pending, :value :x, :equal true}

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
