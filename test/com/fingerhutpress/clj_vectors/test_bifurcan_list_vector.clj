(ns com.fingerhutpress.clj-vectors.test-bifurcan-list-vector
  (:require [clojure.test :as test :refer [deftest testing is are]]
            [com.fingerhutpress.clj-vectors.test-utils]
            [com.fingerhutpress.clj-vectors.test-common :as tc]
            [com.fingerhutpress.clj-vectors.test-long :as tl]
            [clojure.test.check.generators :as gen]
            [collection-check.core :as cc]
            [com.fingerhutpress.clj-vectors.utils :as utils]
            [com.fingerhutpress.clj-vectors.bifurcan-list-vector :as bl]
            [com.fingerhutpress.clj-vectors.bifurcan-list-vector4 :as bl4]))

(set! *warn-on-reflection* true)

;(def num-collection-check-tests 1)  ;; smoke test
;(def num-collection-check-tests 10)  ;; very short
(def num-collection-check-tests 500)
;(def num-collection-check-tests 1000)  ;; medium ~12 sec
;(def num-collection-check-tests 10000)  ;; long test ~2 min
;(def num-collection-check-tests 100000)  ;; very long test

(def empty-vector-constant (bl/vector))

(defn seq->vec [s]
  (into empty-vector-constant s))

(def test-config
  {:seq->vec seq->vec
   :test-subvec bl/subvec
   :test-catvec bl/catvec
   :same-coll? utils/same-coll?
   :height bl/height
   :max-supported-height bl/max-supported-height
   :lg-fullness bl/lg-fullness})

(deftest test-common-bifurcan
  (println "Details of Bifurcan List (default 32) implementation:")
  (bl/print-library-info)
  (tc/test-all-common test-config))

(deftest test-long-bifurcan
  (tl/test-all-long test-config))

(deftest test-vector-like
  (println "assert-vector-like for Bifurcan List(32) with num-collection-check-tests="
           num-collection-check-tests)
  (cc/assert-vector-like num-collection-check-tests empty-vector-constant gen/int))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def empty-vector-constant4 (bl4/vector))

(defn seq->vec4 [s]
  (into empty-vector-constant4 s))

(def test-config4
  {:seq->vec seq->vec4
   :test-subvec bl4/subvec
   :test-catvec bl4/catvec
   :same-coll? utils/same-coll?
   :height bl4/height
   :max-supported-height bl4/max-supported-height
   :lg-fullness bl4/lg-fullness})

(deftest test-common-bifurcan4
  (println "Details of Bifurcan List4 implementation:")
  (bl4/print-library-info)
  (tc/test-all-common test-config4))

(deftest test-long-bifurcan4
  (tl/test-all-long test-config4))

(deftest test-vector-like4
  (println "assert-vector-like for Bifurcan List4 with num-collection-check-tests="
           num-collection-check-tests)
  (cc/assert-vector-like num-collection-check-tests empty-vector-constant4 gen/int))



(comment

(require '[clojure.test :as t])
(require '[com.fingerhutpress.clj-vectors.test-bifurcan-list-vector :as tbl])
(tbl/test-common-bifurcan)

)
