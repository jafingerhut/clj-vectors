(ns com.fingerhutpress.clj-vectors.test-clj-paguro-vector
  (:require [clojure.test :as test :refer [deftest testing is are]]
            [com.fingerhutpress.clj-vectors.test-utils]
            [clojure.test.check.generators :as gen]
            [collection-check.core :as cc]
            [com.fingerhutpress.clj-vectors.clj-paguro-vector :as cp]
            [com.fingerhutpress.clj-vectors.clj-paguro-vector8 :as cp8]))

(set! *warn-on-reflection* true)

;(def num-tests 1)  ;; smoke test
;(def num-tests 10)  ;; very short
(def num-tests 1000)  ;; medium ~12 sec
;(def num-tests 10000)  ;; long test ~2 min
;(def num-tests 100000)  ;; very long test

(deftest test-vector-like
  (println "assert-vector-like for RrbTree (32) with num-tests=" num-tests)
  (cc/assert-vector-like num-tests (cp/vector) gen/int))

(deftest test-vector-like8
  (println "assert-vector-like for RrbTree8 with num-tests=" num-tests)
  (cc/assert-vector-like num-tests (cp8/vector) gen/int))
