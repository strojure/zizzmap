# vzmap

Lazy-map implementation for Clojure(Script).

[![cljdoc badge](https://cljdoc.org/badge/com.github.strojure/vzmap)](https://cljdoc.org/d/com.github.strojure/vzmap)
[![Clojars Project](https://img.shields.io/clojars/v/com.github.strojure/vzmap.svg)](https://clojars.org/com.github.strojure/vzmap)

## Motivation

* Access map values with delayed evaluation as ordinary map values.
* Pass maps with delayed values to code which should not care that values are
  delayed.
