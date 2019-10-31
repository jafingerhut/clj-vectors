(do
(set! *warn-on-reflection* true)
(require '[collection-check.core :as cc]
         ;;'[collection-check.core-test :as ct]
         '[clojure.test.check.generators :as gen])
(require '[com.fingerhutpress.clj-vectors.clj-paguro-vector :as cp]
         '[com.fingerhutpress.clj-vectors.test-clj-paguro-vector :as tpv]
         )
(require '[com.fingerhutpress.clj-vectors.test-common :as tc]
         '[com.fingerhutpress.clj-vectors.utils :as utils])
(def seq->vec tpv/paguro-seq->vec)
(def test-catvec cp/catvec)
)

(defn test-hasheq
  [{:keys [empty-vector seq->vec test-catvec] :as opts}]
  (is (= (hash []) (hash (empty-vector))))  ;; CRRBV-25

(def v1 (seq->vec (range 1024)))
(def v2 (seq->vec (range 1024)))
(def v3 (test-catvec (seq->vec (range 512)) (seq->vec (range 512 1024))))
(def s1 (seq v1))
(def s2 (seq v2))
(def s3 (seq v3))

(= v1 s1)
(= s1 v1)
(hash v1)
(hash s1)
(class s1)

    (is (= (hash v1) (hash v2) (hash v3) (hash s1) (hash s2) (hash s3)))
    (is (= (hash (nthnext s1 120))
           (hash (nthnext s2 120))
           (hash (nthnext s3 120))))))

(def v100 (tpv/paguro-seq->vec (range 100)))
(def v50 (tpv/paguro-seq->vec (range 200 250)))
v100
v50
(class v100)
(count v100)
(instance? clojure.lang.IEditableCollection v100)
(cp/subvec v100 27 81)
(def v150 (cp/catvec v100 v50))
(count v150)
(class v150)
v150

(def v100 (tpv/paguro-seq->vec (vec (range 100))))
v100

(tc/test-all-common tpv/paguro-test-config)
(tc/test-relaxed tpv/paguro-test-config)

(def s1 (concat (range 123) (range 68) (range 64)))

(let [{:keys [test-catvec seq->vec]} tpv/paguro-test-config]
  (def s2 (into (test-catvec (seq->vec (range 123)) (seq->vec (range 68)))
                (range 64))))

(def s2 (seq->vec (range 123)))
(def s3 (seq->vec (range 68)))
s2
s3
(def s4 (test-catvec s2 s3))
s4

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; reproduction of bug in Paguro without modifications, and no Clojure
;; wrappers

;; Reported as Github issue:
;; https://github.com/GlenKPeterson/Paguro/issues/31

(def empty-vector (org.organicdesign.fp.collections.RrbTree/empty))
(class empty-vector)

(defn append-elems-to-vec [v s]
  (reduce (fn [v elem]
            (.append v elem))
          v
          s))

(def v1 (append-elems-to-vec empty-vector (range 44)))
(class v1)
v1
(def v2 (append-elems-to-vec empty-vector (range 45)))
(def v3 (.join v1 v2))
(class v3)
v3
(def v4 (append-elems-to-vec v3 '(-1)))
v4
(.get v4 0)
;; return -1, which has been put at the beginning of v4 instead of the end
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn catvec-then-conj-fails? [m n]
  (let [v1 (seq->vec (range m))
        v2 (seq->vec (range n))
        v3 (test-catvec v1 v2)
        v4 (into v3 [-1])]
    (= -1 (get v4 0))))

(def x
(for [m (range 44 45)
      n (range 1 100)
      :when (catvec-then-conj-fails? m n)]
  [m n])
)
(count x)
(- 99 44)

(catvec-then-conj-fails? 10 10)
(catvec-then-conj-fails? 123 68)

(def s5 (into s4 (range 64)))
s5
(get (into s4 [-5]) 0)
(get (into s4 [-5]) 1)
(.v s4)
(.append (.append (.v s4) -5) -7)

(def s2 (seq->vec (range 0 10)))
(def s3 (seq->vec (range 100 110)))
(def s4 (test-catvec s2 s3))
s4
(def s5 (into s4 [-1]))
s5

#_(do
(require '[com.fingerhutpress.clj-vectors.cljify-jvm-vector :as c])
(import '(org.organicdesign.fp.collections
          RrbTree
          RrbTree$ImRrbt))
)


(def v0 (cp/vector))
(class v0)
(count v0)
(instance? clojure.lang.IEditableCollection v0)
(.size v0)
(= [] v0)
(= v0 [])
(cp/vector 1 2 3 4)
(def v20 (cp/vec (range 20)))
(class v20)
v20
(cp/subvec v20 0 20)
(class (cp/subvec v20 0 20))
(cp/subvec v20 1 18)
(class (cp/subvec v20 1 18))
(count (cp/subvec v20 1 18))
(cp/subvec v20 0 21)
;; IndexOutOfBoundsException
(cp/subvec v20 -1 10)
;; IndexOutOfBoundsException
(cp/subvec v20 20 10)
;; IndexOutOfBoundsException
(cp/subvec v20 10 9)
;; IndexOutOfBoundsException


(class (.v v100))
(def tup (.split (.v v100) 5))
tup
(class (._1 tup))
(class (._2 tup))

(def v20 (vec (range 20)))
(class v20)
(subvec v20 0 20)
;; ok
(subvec v20 0 21)
;; IndexOutOfBoundsException
(subvec v20 -1 10)
;; IndexOutOfBoundsException
(subvec v20 20 10)
;; IndexOutOfBoundsException
(subvec v20 10 9)
;; IndexOutOfBoundsException

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
