(ns project.readme.core-03-merge
  (:require [strojure.zizzmap.core :as zizz]))

(def ^:private -merged
  (zizz/merge* {:a 1}
               (zizz/init {:b (do (println "Init") 2)})))

(get -merged :a)
;=> 1

(get -merged :b)
;Init
;=> 2