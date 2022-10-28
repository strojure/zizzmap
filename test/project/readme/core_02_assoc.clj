(ns project.readme.core-02-assoc
  (:require [strojure.zizzmap.core :as zizz]))

(def ^:private -map
  (zizz/assoc* {} :a (do (println "Init") 1)))

(get -map :a)
;Init
;=> 1