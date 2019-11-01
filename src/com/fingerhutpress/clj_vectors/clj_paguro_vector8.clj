(ns com.fingerhutpress.clj-vectors.clj-paguro-vector8
  (:refer-clojure :exclude [vector vec subvec])
  (:require [com.fingerhutpress.clj-vectors.cljify-jvm-vector :as c])
  (:import (org.organicdesign.fp.collections
            RrbTree8
            RrbTree8$ImRrbt)
           (org.organicdesign.fp.tuple
            Tuple2)))

;; This is intended to be identical to namespace
;; com.fingerhutpress.clj-vectors.clj-paguro-vector, except that it
;; uses the class RrbTree8, which has a smaller tree branch factor of
;; 8, instead of RrbTree, which has a branch factor of 32.  The hope
;; is that shorter and smaller test cases will be more likely to find
;; bugs in this version, vs. the original.

(set! *warn-on-reflection* true)

(def paguro-empty-vector-constant (RrbTree8/empty))

(defn- paguro-empty-vec []
  paguro-empty-vector-constant)

(defn- paguro-java-hashcode [^RrbTree8$ImRrbt v]
  (.hashCode v))

(defn- paguro-java-tostring [^RrbTree8$ImRrbt v]
  (.toString v))

(defn- paguro-get-nth [^RrbTree8$ImRrbt v idx]
  (.get v idx))

(defn- paguro-append-one-elem [^RrbTree8$ImRrbt v elem]
  (.append v elem))

(defn- paguro-remove-last-elem [^RrbTree8$ImRrbt v]
  (let [idx (dec (.size v))]
    (.without v idx)))

(defn- paguro-replace-elem [^RrbTree8$ImRrbt v idx new-elem]
  (let [i (int idx)]
    (.replace v i new-elem)))

(c/generate-vector-type PaguroRrbVector
                        PaguroRrbVecSeq
                        paguro-empty-vec
                        paguro-java-hashcode
                        paguro-java-tostring
                        paguro-get-nth
                        paguro-append-one-elem
                        paguro-remove-last-elem
                        paguro-replace-elem)

(defn vector [& elems]
  (let [v (PaguroRrbVector. paguro-empty-vector-constant 0 nil 0 0)]
    (reduce conj v elems)))

(defn vec [coll]
  (apply vector coll))

(defn- paguro-subvec [^RrbTree8$ImRrbt v start end size]
  (if (<= 0 start end size)
    (if (== start end)
      paguro-empty-vector-constant
      (let [^Tuple2 tup1 (.split v end)
            ^RrbTree8$ImRrbt before-end (._1 tup1)
            ^Tuple2 tup2 (.split before-end start)]
        (._2 tup2)))
    (throw (IndexOutOfBoundsException.))))

(defn subvec
  ([vec start]
   (subvec vec start (count vec)))
  ([^PaguroRrbVector vec start end]
   (PaguroRrbVector. (paguro-subvec (.v vec) start end (count vec))
                     (- end start) (meta vec) 0 0)))

(defn- splicev [^PaguroRrbVector v1 ^PaguroRrbVector v2]
  (assert (instance? PaguroRrbVector v1))
  (assert (instance? PaguroRrbVector v2))
  (when-not (instance? RrbTree8$ImRrbt (.v v1))
    (println "----------------------------------------------------------------------")
    (println "(class (.v v1))=" (class (.v v1)))
    (flush)
    (assert (instance? RrbTree8$ImRrbt (.v v1))))
  (when-not (instance? RrbTree8$ImRrbt (.v v2))
    (println "----------------------------------------------------------------------")
    (println "(class (.v v2))=" (class (.v v2)))
    (flush)
    (assert (instance? RrbTree8$ImRrbt (.v v2))))
  (let [^RrbTree8$ImRrbt pv1 (.v v1)
        ^RrbTree8$ImRrbt pv2 (.v v2)
        c (.join pv1 pv2)]
    (assert (instance? RrbTree8$ImRrbt c))
    (PaguroRrbVector. c (+ (count v1) (count v2))
                      ;; TBD: Not clear which metadata should be on
                      ;; returned vector, if any.  Perhaps nil would be
                      ;; more appropriate.
                      (meta v1) 0 0)))

(defn catvec
  "Concatenates the given vectors."
  ([]
     [])
  ([v1]
     v1)
  ([v1 v2]
     (splicev v1 v2))
  ([v1 v2 v3]
     (splicev (splicev v1 v2) v3))
  ([v1 v2 v3 v4]
     (splicev (splicev v1 v2) (splicev v3 v4)))
  ([v1 v2 v3 v4 & vn]
     (splicev (splicev (splicev v1 v2) (splicev v3 v4))
              (apply catvec vn))))

(comment

(import '(org.organicdesign.fp.collections
          RrbTree8
          RrbTree8$ImRrbt
          RrbTree8$MutableRrbt))

(def v0 (RrbTree8/empty))
(def v1 (RrbTree8$ImRrbt/empty))
(def v2 (RrbTree8$MutableRrbt/empty))

(class v0)
;; org.organicdesign.fp.collections.RrbTree8$ImRrbt
(= (class v0) (class v1) (class v2))
;; true

)
