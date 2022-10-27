# zizzmap

Lazy-map implementation for Clojure(Script).

[![cljdoc badge](https://cljdoc.org/badge/com.github.strojure/zizzmap)](https://cljdoc.org/d/com.github.strojure/zizzmap)
[![Clojars Project](https://img.shields.io/clojars/v/com.github.strojure/zizzmap.svg)](https://clojars.org/com.github.strojure/zizzmap)

## Motivation

* Access map values with delayed evaluation as ordinary map values.
* Pass maps with delayed values to code which should not care that values are
  delayed.
