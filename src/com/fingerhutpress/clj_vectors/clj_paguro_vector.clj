(ns com.fingerhutpress.clj-vectors.clj-paguro-vector
  (:refer-clojure :exclude [vector vec subvec])
  (:require [com.fingerhutpress.clj-vectors.cljify-jvm-vector :as c])
  (:import (org.organicdesign.fp.collections
            RrbTree
            RrbTree$ImRrbt)
           (org.organicdesign.fp.tuple
            Tuple2)))

(set! *warn-on-reflection* true)

(def paguro-empty-vector-constant (RrbTree/empty))

(defn- paguro-empty-vec []
  paguro-empty-vector-constant)

(defn- paguro-java-hashcode [^RrbTree$ImRrbt v]
  (.hashCode v))

(defn- paguro-java-tostring [^RrbTree$ImRrbt v]
  (.toString v))

(defn- paguro-get-nth [^RrbTree$ImRrbt v idx]
  (.get v idx))

(defn- paguro-append-one-elem [^RrbTree$ImRrbt v elem]
  (.append v elem))

(defn- paguro-remove-last-elem [^RrbTree$ImRrbt v]
  (let [idx (dec (.size v))]
    (.without v idx)))

(defn- paguro-replace-elem [^RrbTree$ImRrbt v idx new-elem]
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

(defn- paguro-subvec [^RrbTree$ImRrbt v start end size]
  (if (<= 0 start end size)
    (if (== start end)
      paguro-empty-vector-constant
      (let [^Tuple2 tup1 (.split v end)
            ^RrbTree$ImRrbt before-end (._1 tup1)
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
  (when-not (instance? RrbTree$ImRrbt (.v v1))
    (println "----------------------------------------------------------------------")
    (println "(class (.v v1))=" (class (.v v1)))
    (flush)
    (assert (instance? RrbTree$ImRrbt (.v v1))))
  (when-not (instance? RrbTree$ImRrbt (.v v2))
    (println "----------------------------------------------------------------------")
    (println "(class (.v v2))=" (class (.v v2)))
    (flush)
    (assert (instance? RrbTree$ImRrbt (.v v2))))
  (let [^RrbTree$ImRrbt pv1 (.v v1)
        ^RrbTree$ImRrbt pv2 (.v v2)
        c (.join pv1 pv2)]
    (assert (instance? RrbTree$ImRrbt c))
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


#_(def vector-implementation-functions
  {:empty-vector (fn paguro-empty-vec []
                   paguro-vector-constant)
   :java-hashcode (fn paguro-java-hashcode [^RrbTree$ImRrbt v]
                    (.hashCode v))
   :java-tostring (fn paguro-java-tostring [^RrbTree$ImRrbt v]
                    (.toString v))
   :get-nth (fn paguro-get-nth [^RrbTree$ImRrbt v idx]
              (.get v idx))
   :append-one-elem (fn paguro-append-one-elem [^RrbTree$ImRrbt v elem]
                      (.append v elem))
   :remove-last-elem (fn paguro-remove-last-elem [^RrbTree$ImRrbt v]
                       (let [idx (dec (.size v))]
                         (.without v idx)))
   :replace-elem (fn paguro-replace-elem [^RrbTree$ImRrbt v idx new-elem]
                   (.replace v idx new-elem))})

(comment

(import '(org.organicdesign.fp.collections
          RrbTree
          RrbTree$ImRrbt
          RrbTree$MutableRrbt))

(def v0 (RrbTree/empty))
(def v1 (RrbTree$ImRrbt/empty))
(def v2 (RrbTree$MutableRrbt/empty))

(class v0)
;; org.organicdesign.fp.collections.RrbTree$ImRrbt
(= (class v0) (class v1) (class v2))
;; true

)
