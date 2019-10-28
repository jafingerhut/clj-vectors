(do
(set! *warn-on-reflection* true)
(require '[collection-check.core :as cc]
         ;;'[collection-check.core-test :as ct]
         '[clojure.test.check.generators :as gen])
(require '[com.fingerhutpress.clj-vectors.clj-paguro-vector :as cp])
)


#_(do
(require '[com.fingerhutpress.clj-vectors.cljify-jvm-vector :as c])
(import '(org.organicdesign.fp.collections
          RrbTree
          RrbTree$ImRrbt))
)

(def v0 (cp/vector))
(class v0)
(count v0)
(.size v0)
(= [] v0)
(= v0 [])
(cp/vector 1 2 3 4)
(cp/vec (range 100))

;(def num-tests 1)  ;; smoke test
;(def num-tests 10)  ;; very short
(def num-tests 1000)  ;; medium
;(def num-tests 10000)  ;; long test
;(def num-tests 100000)  ;; very long test

(require '[clojure.test :as test])

(test/deftest test-paguro-vector
  (println "Before assert-vector-like with num-tests=" num-tests)
  (cc/assert-vector-like num-tests (cp/vector) gen/int)
  (println "After assert-vector-like with num-tests=" num-tests))

(time (test-paguro-vector))


(def i vector-implementation-functions)
(def v1 ((:empty-vector i)))
v1
(def v2 ((:append-one-elem i) v1 5))
v2
((:get-nth i) v2 0)
(def v3 ((:append-one-elem i) v2 7))
v3
((:get-nth i) v3 0)
((:get-nth i) v3 1)


(pprint (macroexpand-1 '(c/generate-vector-type PaguroRrbVector
                                                PaguroRrbVecSeq
                                                vector-implementation-functions)

PaguroRrbVecSeq
user.PaguroRrbVecSeq
(class user.PaguroRrbVecSeq)
;; java.lang.Class


empty-paguro-vec
(= empty-paguro-vec [])
(= [] empty-paguro-vec)

(def x1 (PaguroRrbVecSeq. [1 2 3] 0))
x1

(def e1 *e)
(pst e1 1000)
