(ns com.fingerhutpress.clj-vectors.test-bifurcan-list-vector
  (:require [clojure.test :as test :refer [deftest testing is are]]
            [com.fingerhutpress.clj-vectors.test-utils]
            [com.fingerhutpress.clj-vectors.test-common :as tc]
            [com.fingerhutpress.clj-vectors.test-long :as tl]
            [clojure.test.check.generators :as gen]
            [collection-check.core :as cc]
            [com.fingerhutpress.clj-vectors.utils :as utils]
            [com.fingerhutpress.clj-vectors.bifurcan-list-vector :as bl]
            ;;[com.fingerhutpress.clj-vectors.bifurcan-list-vector4 :as bl4]
            ))

(set! *warn-on-reflection* true)

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


(comment

(require '[clojure.test :as t])
(require '[com.fingerhutpress.clj-vectors.test-bifurcan-list-vector :as tbl])
(tbl/test-common-bifurcan)

)
