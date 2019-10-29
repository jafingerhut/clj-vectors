(ns com.fingerhutpress.clj-vectors.clj-paguro-vector8
  (:refer-clojure :exclude [vector vec subvec])
  (:require [com.fingerhutpress.clj-vectors.cljify-jvm-vector :as c])
  (:import (org.organicdesign.fp.collections
            RrbTree8
            RrbTree8$ImRrbt)))

;; This is intended to be identical to namespace
;; com.fingerhutpress.clj-vectors.clj-paguro-vector8, except that it
;; uses the class RrbTree8, which has a smaller tree branch factor of
;; 8, instead of RrbTree, which has a branch factor of 32.  The hope
;; is that shorter and smaller test cases will be more likely to find
;; bugs in this version, vs. the original.

(set! *warn-on-reflection* true)

(def paguro-empty-vector-constant (RrbTree8/empty))

(defn paguro-empty-vec []
  paguro-empty-vector-constant)

(defn paguro-java-hashcode [^RrbTree8$ImRrbt v]
  (.hashCode v))

(defn paguro-java-tostring [^RrbTree8$ImRrbt v]
  (.toString v))

(defn paguro-get-nth [^RrbTree8$ImRrbt v idx]
  (.get v idx))

(defn paguro-append-one-elem [^RrbTree8$ImRrbt v elem]
  (.append v elem))

(defn paguro-remove-last-elem [^RrbTree8$ImRrbt v]
  (let [idx (dec (.size v))]
    (.without v idx)))

(defn paguro-replace-elem [^RrbTree8$ImRrbt v idx new-elem]
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

;; TBD: Define functions subvec and catvec


#_(def vector-implementation-functions
  {:empty-vector (fn paguro-empty-vec []
                   paguro-vector-constant)
   :java-hashcode (fn paguro-java-hashcode [^RrbTree8$ImRrbt v]
                    (.hashCode v))
   :java-tostring (fn paguro-java-tostring [^RrbTree8$ImRrbt v]
                    (.toString v))
   :get-nth (fn paguro-get-nth [^RrbTree8$ImRrbt v idx]
              (.get v idx))
   :append-one-elem (fn paguro-append-one-elem [^RrbTree8$ImRrbt v elem]
                      (.append v elem))
   :remove-last-elem (fn paguro-remove-last-elem [^RrbTree8$ImRrbt v]
                       (let [idx (dec (.size v))]
                         (.without v idx)))
   :replace-elem (fn paguro-replace-elem [^RrbTree8$ImRrbt v idx new-elem]
                   (.replace v idx new-elem))})

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
