(ns project.readme.core-05-delay
  (:require [strojure.zizzmap.core :as zizz]))

(def ^:private -map
  (-> {:a (zizz/delay* (println "Init")
                       1)}
      (zizz/convert-map)))

(get -map :a)
;Init
;=> 1
