(ns strojure.zizzmap.core-test
  (:require [clojure.test :as test :refer [deftest]]
            [strojure.zizzmap.core :as zizz]
            [strojure.zizzmap.impl :as impl]))

#?(:clj  (set! *warn-on-reflection* true)
   :cljs (set! *warn-on-infer* true))

(declare thrown?)

#_(test/run-tests)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest init-t
  (test/are [expr result] (= result expr)

    (let [a (atom :pending)
          m (zizz/init ^::meta {:a (do (reset! a :realized)
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
          m (zizz/assoc* ^::meta {} :a (do (reset! a :realized)
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
          m (zizz/merge* ^::meta {:b :y}
                         (zizz/init {:a (do (reset! a :realized)
                                            :x)}))]
      {:persistent (impl/persistent? m)
       :atom @a
       :value (:a m)
       :meta (meta m)
       :equal (= m {:a :x :b :y})})
    {:persistent true, :atom :pending, :value :x, :meta {::meta true}, :equal true}

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest update*-t
  (test/are [expr result] (= result expr)

    (let [a (atom :pending)
          m (-> (zizz/init ^::meta {:a (do (reset! a :realized)
                                           :x)})
                (zizz/update* :a name))]
      {:persistent (impl/persistent? m)
       :atom @a
       :value (:a m)
       :meta (meta m)
       :equal (= m {:a "x"})})
    {:persistent true, :atom :pending, :value "x", :meta {::meta true}, :equal true}

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
