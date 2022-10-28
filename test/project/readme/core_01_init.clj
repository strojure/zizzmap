(ns project.readme.core-01-init
  (:require [strojure.zizzmap.core :as zizz]))

(def ^:private -map
  (zizz/init {:a (do (println "Init") 1)}))

(get -map :a)
;Init
;=> 1