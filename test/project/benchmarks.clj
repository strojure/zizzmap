(ns project.benchmarks
  (:require [lazy-map.core :as lazy]
            [strojure.zizzmap.core :as zizz]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def -core {:a 1})
(def -zizz (zizz/init {:a 1}))
(def -lazy (lazy/lazy-map {:a 1}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; Get existing value

(get -core :a)
;             Execution time mean : 8,433409 ns
;    Execution time std-deviation : 0,469744 ns
;   Execution time lower quantile : 7,879697 ns ( 2,5%)
;   Execution time upper quantile : 8,979944 ns (97,5%)

(get -zizz :a)                                    ; 50% slower
;             Execution time mean : 12,947202 ns
;    Execution time std-deviation : 0,321168 ns
;   Execution time lower quantile : 12,637812 ns ( 2,5%)
;   Execution time upper quantile : 13,378630 ns (97,5%)

(get -lazy :a)
;             Execution time mean : 15,024707 ns
;    Execution time std-deviation : 0,484878 ns
;   Execution time lower quantile : 14,434105 ns ( 2,5%)
;   Execution time upper quantile : 15,612451 ns (97,5%)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; IFn existing value

(-core :a)
;             Execution time mean : 7,252501 ns
;    Execution time std-deviation : 0,602015 ns
;   Execution time lower quantile : 6,717723 ns ( 2,5%)
;   Execution time upper quantile : 8,262110 ns (97,5%)

(-zizz :a)                                        ; 35% slower
;             Execution time mean : 9,833216 ns
;    Execution time std-deviation : 0,351018 ns
;   Execution time lower quantile : 9,527257 ns ( 2,5%)
;   Execution time upper quantile : 10,353496 ns (97,5%)

(comment
  (-lazy :a))
;class lazy_map.core.LazyMap cannot be cast to class clojure.lang.IFn

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; Get missing value

(get -core :b)
;             Execution time mean : 4,654095 ns
;    Execution time std-deviation : 0,386965 ns
;   Execution time lower quantile : 4,257729 ns ( 2,5%)
;   Execution time upper quantile : 5,103697 ns (97,5%)

(get -zizz :b)
;             Execution time mean : 5,641230 ns
;    Execution time std-deviation : 0,338573 ns
;   Execution time lower quantile : 5,255823 ns ( 2,5%)
;   Execution time upper quantile : 5,982961 ns (97,5%)

(get -lazy :b)
;             Execution time mean : 9,693672 ns
;    Execution time std-deviation : 0,840593 ns
;   Execution time lower quantile : 9,123320 ns ( 2,5%)
;   Execution time upper quantile : 11,103573 ns (97,5%)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; Initialization cost

{:a 1}
;             Execution time mean : 3,340801 ns
;    Execution time std-deviation : 0,165079 ns
;   Execution time lower quantile : 3,166035 ns ( 2,5%)
;   Execution time upper quantile : 3,541582 ns (97,5%)

(zizz/init {:a 1})
;             Execution time mean : 85,179657 ns
;    Execution time std-deviation : 25,474225 ns
;   Execution time lower quantile : 60,439248 ns ( 2,5%)
;   Execution time upper quantile : 121,507634 ns (97,5%)

(lazy/lazy-map {:a 1})
;             Execution time mean : 48,246205 ns
;    Execution time std-deviation : 17,454193 ns
;   Execution time lower quantile : 34,441823 ns ( 2,5%)
;   Execution time upper quantile : 70,415865 ns (97,5%)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; assoc

(assoc -core :b 2)
;             Execution time mean : 46,985973 ns
;    Execution time std-deviation : 1,926830 ns
;   Execution time lower quantile : 44,364049 ns ( 2,5%)
;   Execution time upper quantile : 48,689632 ns (97,5%)

(assoc -zizz :b 2)
;             Execution time mean : 91,242760 ns
;    Execution time std-deviation : 21,777259 ns
;   Execution time lower quantile : 74,655095 ns ( 2,5%)
;   Execution time upper quantile : 116,879852 ns (97,5%)

(assoc -lazy :b 2)
;             Execution time mean : 55,723321 ns
;    Execution time std-deviation : 14,859886 ns
;   Execution time lower quantile : 48,254482 ns ( 2,5%)
;   Execution time upper quantile : 81,482730 ns (97,5%)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
