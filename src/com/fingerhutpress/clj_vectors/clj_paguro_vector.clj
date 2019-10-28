(ns com.fingerhutpress.clj-vectors.clj-paguro-vector
  (:refer-clojure :exclude [vector vec subvec])
  (:require [com.fingerhutpress.clj-vectors.cljify-jvm-vector :as c])
  (:import (org.organicdesign.fp.collections
            RrbTree
            RrbTree$ImRrbt)))

(set! *warn-on-reflection* true)

(def paguro-empty-vector-constant (RrbTree/empty))

(defn paguro-empty-vec []
  paguro-empty-vector-constant)

(defn paguro-java-hashcode [^RrbTree$ImRrbt v]
  (.hashCode v))

(defn paguro-java-tostring [^RrbTree$ImRrbt v]
  (.toString v))

(defn paguro-get-nth [^RrbTree$ImRrbt v idx]
  (.get v idx))

(defn paguro-append-one-elem [^RrbTree$ImRrbt v elem]
  (.append v elem))

(defn paguro-remove-last-elem [^RrbTree$ImRrbt v]
  (let [idx (dec (.size v))]
    (.without v idx)))

(defn paguro-replace-elem [^RrbTree$ImRrbt v idx new-elem]
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
