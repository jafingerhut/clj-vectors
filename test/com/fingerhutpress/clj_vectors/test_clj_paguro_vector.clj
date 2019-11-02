(ns com.fingerhutpress.clj-vectors.test-clj-paguro-vector
  (:require [clojure.test :as test :refer [deftest testing is are]]
            [com.fingerhutpress.clj-vectors.test-utils]
            [com.fingerhutpress.clj-vectors.test-common :as tc]
            [com.fingerhutpress.clj-vectors.test-long :as tl]
            [clojure.test.check.generators :as gen]
            [collection-check.core :as cc]
            [com.fingerhutpress.clj-vectors.utils :as utils]
            [com.fingerhutpress.clj-vectors.clj-paguro-vector :as cp]
            [com.fingerhutpress.clj-vectors.clj-paguro-vector8 :as cp8]))

(set! *warn-on-reflection* true)

;(def num-collection-check-tests 1)  ;; smoke test
;(def num-collection-check-tests 10)  ;; very short
(def num-collection-check-tests 500)
;(def num-collection-check-tests 1000)  ;; medium ~12 sec
;(def num-collection-check-tests 10000)  ;; long test ~2 min
;(def num-collection-check-tests 100000)  ;; very long test

(def empty-vector-constant (cp/vector))

(defn paguro-seq->vec [s]
  (into empty-vector-constant s))

(def paguro-test-config
  {:seq->vec paguro-seq->vec
   :test-subvec cp/subvec
   :test-catvec cp/catvec
   :same-coll? utils/same-coll?})

(deftest test-common-paguro
  (tc/test-all-common paguro-test-config))

(deftest test-long-paguro
  (tl/test-all-long paguro-test-config))

(deftest test-vector-like
  (println "assert-vector-like for RrbTree(32) with num-collection-check-tests="
           num-collection-check-tests)
  (cc/assert-vector-like num-collection-check-tests (cp/vector) gen/int))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def empty-vector-constant8 (cp8/vector))

(defn paguro-seq->vec8 [s]
  (into empty-vector-constant8 s))

(def paguro-test-config8
  {:seq->vec paguro-seq->vec8
   :test-subvec cp8/subvec
   :test-catvec cp8/catvec
   :same-coll? utils/same-coll?})

(deftest test-common-paguro8
  (tc/test-all-common paguro-test-config8))

(deftest test-long-paguro8
  (tl/test-all-long paguro-test-config8))

(deftest test-vector-like8
  (println "assert-vector-like for RrbTree8 with num-collection-check-tests="
           num-collection-check-tests)
  (cc/assert-vector-like num-collection-check-tests (cp8/vector) gen/int))
