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
          m (map/init ^::meta {:a (do (reset! a :realized)
                                      :x)})]
      {:persistent (impl/persistent? m)
       :atom @a
       :value (:a m)
       :meta (meta m)
       :equal (= m {:a :x})})
    {:persistent true, :atom :pending, :value :x, :meta {::meta true}, :equal true}

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest assoc*-t
  (test/are [expr result] (= result expr)

    (let [a (atom :pending)
          m (map/assoc* ^::meta {} :a (do (reset! a :realized)
                                          :x))]
      {:persistent (impl/persistent? m)
       :atom @a
       :value (:a m)
       :meta (meta m)
       :equal (= m {:a :x})})
    {:persistent true, :atom :pending, :value :x, :meta {::meta true}, :equal true}

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest merge*-t
  (test/are [expr result] (= result expr)

    (let [a (atom :pending)
          m (map/merge* ^::meta {:b :y}
                        (map/init {:a (do (reset! a :realized)
                                          :x)}))]
      {:persistent (impl/persistent? m)
       :atom @a
       :value (:a m)
       :meta (meta m)
       :equal (= m {:a :x :b :y})})
    {:persistent true, :atom :pending, :value :x, :meta {::meta true}, :equal true}

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
