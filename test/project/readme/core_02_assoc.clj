(ns project.readme.core-02-assoc
  (:require [strojure.zizzmap.core :as zizz]))

(def ^:private -map1
  (zizz/assoc* {} :a (do (println "Init") 1)))

(get -map1 :a)
;Init
;=> 1

(def ^:private -map2
  (zizz/assoc* {}
               :a (do (println "Init :a") 1)
               :b (do (println "Init :b") 2)))

(get -map2 :b)
;Init
;=> 2