# zizzmap

Persistent map with lazily evaluated values for Clojure(Script).

[![cljdoc badge](https://cljdoc.org/badge/com.github.strojure/zizzmap)](https://cljdoc.org/d/com.github.strojure/zizzmap)
[![Clojars Project](https://img.shields.io/clojars/v/com.github.strojure/zizzmap.svg)](https://clojars.org/com.github.strojure/zizzmap)
![Deprecated](https://img.shields.io/badge/status-deprecated-red)

`zizz` (noun) A nap.

## Status

**DEPRECATED.** Use https://github.com/strojure/zmap with more explicit API and
practical naming.

---

## Motivation

* Access map values with delayed evaluation as ordinary map values.
* Pass maps with delayed values to code which should not care that values are
  delayed.

## Features

* Keep pending delayed values not realized until used.
* Do not mix zizzmap-delays with delays created by `clojure.core/delay`.
* Support `IFn` interface of persistent map.
* Support transients.
* Transparent IPersistentMap behaviour with minimal overhead.

## Where to use

The `zizzmap` can be used when large map need to be created but most of the map
keys will never be used. For example ring request map with multiple conversions
from HTTP request to Clojure data structure.

## API

### `init`

The macro `init` is used to define map with delayed values.

```clojure
(ns project.readme.core-01-init
  (:require [strojure.zizzmap.core :as zizz]))

(def ^:private -map
  (zizz/init {:a (do (println "Init") 1)}))

(get -map :a)
;Init
;=> 1
```

### `assoc*`

The macro `assoc*` returns map with delayed value at specified key.

```clojure
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
```

### `merge*`

The function `merge*` is like `clojure.core/merge` but keeps pending values not
realized.

```clojure
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
```

### `update*`

The function `update*` is like `clojure.core/update` but delays application of
function `f` to map value.

```clojure
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
```

### `delay*`

The map can be constructed manually using `delay*` and `convert-map`.

```clojure
(ns project.readme.core-05-delay
  (:require [strojure.zizzmap.core :as zizz]))

(def ^:private -map
  (-> {:a (zizz/delay* (println "Init")
                       1)}
      (zizz/convert-map)))

(get -map :a)
;Init
;=> 1
```

## Performance

See some benchmarks [here](test/project/benchmarks.clj).
