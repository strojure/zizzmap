(ns project.benchmarks
  (:require [lazy-map.core :as lazy]
            [strojure.zizzmap.core :as zizz]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def -core {:a 1})
(def -zizz (zizz/init {:a (identity 1)}))
(def -lazy (lazy/lazy-map {:a 1}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; Get existing value

(get -core :a)
;             Execution time mean : 10,631153 ns
;    Execution time std-deviation : 0,214500 ns
;   Execution time lower quantile : 10,476636 ns ( 2,5%)
;   Execution time upper quantile : 10,991837 ns (97,5%)

(get -zizz :a)
;             Execution time mean : 10,826143 ns
;    Execution time std-deviation : 0,107145 ns
;   Execution time lower quantile : 10,680422 ns ( 2,5%)
;   Execution time upper quantile : 10,946770 ns (97,5%)

(get -lazy :a)
;             Execution time mean : 15,024707 ns
;    Execution time std-deviation : 0,484878 ns
;   Execution time lower quantile : 14,434105 ns ( 2,5%)
;   Execution time upper quantile : 15,612451 ns (97,5%)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; IFn existing value

(-core :a)
;             Execution time mean : 6,564809 ns
;    Execution time std-deviation : 0,170354 ns
;   Execution time lower quantile : 6,376178 ns ( 2,5%)
;   Execution time upper quantile : 6,745566 ns (97,5%)

(-zizz :a)                                        ; 35% slower
;             Execution time mean : 8,892992 ns
;    Execution time std-deviation : 0,235938 ns
;   Execution time lower quantile : 8,553368 ns ( 2,5%)
;   Execution time upper quantile : 9,156227 ns (97,5%)

(comment
  (-lazy :a))
;class lazy_map.core.LazyMap cannot be cast to class clojure.lang.IFn

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; Get missing value

(get -core :b)
;             Execution time mean : 3,114547 ns
;    Execution time std-deviation : 0,113178 ns
;   Execution time lower quantile : 2,985051 ns ( 2,5%)
;   Execution time upper quantile : 3,249071 ns (97,5%)

(get -zizz :b)
;             Execution time mean : 4,316668 ns
;    Execution time std-deviation : 0,110105 ns
;   Execution time lower quantile : 4,165729 ns ( 2,5%)
;   Execution time upper quantile : 4,413324 ns (97,5%)

(get -lazy :b)
;             Execution time mean : 7,664370 ns
;    Execution time std-deviation : 0,245763 ns
;   Execution time lower quantile : 7,327563 ns ( 2,5%)
;   Execution time upper quantile : 7,901659 ns (97,5%)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; Initialization cost

{:a 1}
;             Execution time mean : 3,340801 ns
;    Execution time std-deviation : 0,165079 ns
;   Execution time lower quantile : 3,166035 ns ( 2,5%)
;   Execution time upper quantile : 3,541582 ns (97,5%)

(zizz/init {:a 1})
;             Execution time mean : 7,241391 ns
;    Execution time std-deviation : 1,001237 ns
;   Execution time lower quantile : 6,549447 ns ( 2,5%)
;   Execution time upper quantile : 8,946173 ns (97,5%)

(zizz/init {:a (identity 1)})
;             Execution time mean : 40,740169 ns
;    Execution time std-deviation : 5,418957 ns
;   Execution time lower quantile : 36,433236 ns ( 2,5%)
;   Execution time upper quantile : 46,921194 ns (97,5%)

(lazy/lazy-map {:a 1})
;             Execution time mean : 37,309820 ns
;    Execution time std-deviation : 4,009381 ns
;   Execution time lower quantile : 33,804579 ns ( 2,5%)
;   Execution time upper quantile : 42,172047 ns (97,5%)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; assoc

(assoc -core :b 2)
;             Execution time mean : 45,786820 ns
;    Execution time std-deviation : 0,876607 ns
;   Execution time lower quantile : 44,592850 ns ( 2,5%)
;   Execution time upper quantile : 46,794216 ns (97,5%)

(assoc -zizz :b 2)
;             Execution time mean : 48,847897 ns
;    Execution time std-deviation : 1,644363 ns
;   Execution time lower quantile : 47,081225 ns ( 2,5%)
;   Execution time upper quantile : 50,683465 ns (97,5%)

(assoc -lazy :b 2)
;             Execution time mean : 49,034480 ns
;    Execution time std-deviation : 3,042376 ns
;   Execution time lower quantile : 46,031159 ns ( 2,5%)
;   Execution time upper quantile : 53,893994 ns (97,5%)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
