(ns project.readme.core-04-update
  (:require [strojure.zizzmap.core :as zizz]))

;;; Update delayed value

(def ^:private -map1
  (-> (zizz/init {:a (do (println "Init") 1)})
      (zizz/update* :a inc)))

(get -map1 :a)
;Init
;=> 2

;;; Delayed update in standard map

(def ^:private -map2
  (-> {:a 1}
      (zizz/update* :a (fn [a]
                         (println "Update")
                         (inc a)))))

(get -map2 :a)
;Update
;=> 2
