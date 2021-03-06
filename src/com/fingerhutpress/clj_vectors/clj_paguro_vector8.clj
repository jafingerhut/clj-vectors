(ns com.fingerhutpress.clj-vectors.clj-paguro-vector8
  (:refer-clojure :exclude [vector vec subvec])
  (:require [com.fingerhutpress.clj-vectors.cljify-jvm-vector :as c]
            [com.fingerhutpress.clj-vectors.utils :as utils])
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

(def rrbtree-class org.organicdesign.fp.collections.RrbTree8)
(def node-length-pow-2
  (utils/get-static-int-field rrbtree-class "NODE_LENGTH_POW_2"))
(def strict-node-length
  (utils/get-static-int-field rrbtree-class "STRICT_NODE_LENGTH"))
(def half-strict-node-length
  (utils/get-static-int-field rrbtree-class "HALF_STRICT_NODE_LENGTH"))
(def min-node-length
  (utils/get-static-int-field rrbtree-class "MIN_NODE_LENGTH"))
(def max-node-length
  (utils/get-static-int-field rrbtree-class "MAX_NODE_LENGTH"))

(def empty-vector-constant (RrbTree8/empty))

(defn- empty-vec []
  empty-vector-constant)

(defn- java-hashcode [^RrbTree8$ImRrbt v]
  (.hashCode v))

(defn- java-tostring [^RrbTree8$ImRrbt v]
  (.toString v))

(defn- get-nth [^RrbTree8$ImRrbt v idx]
  (.get v idx))

(defn- append-one-elem [^RrbTree8$ImRrbt v elem]
  (.append v elem))

(defn- remove-last-elem [^RrbTree8$ImRrbt v]
  (let [idx (dec (.size v))]
    (.without v idx)))

(defn- replace-elem [^RrbTree8$ImRrbt v idx new-elem]
  (let [i (int idx)]
    (.replace v i new-elem)))

(c/generate-vector-type PaguroRrbVector
                        PaguroRrbVecSeq
                        empty-vec
                        java-hashcode
                        java-tostring
                        get-nth
                        append-one-elem
                        remove-last-elem
                        replace-elem)

(defn vector [& elems]
  (let [v (PaguroRrbVector. empty-vector-constant 0 nil 0 0)]
    (reduce conj v elems)))

(defn vec [coll]
  (apply vector coll))

(defn- subvec* [^RrbTree8$ImRrbt v start end size]
  (if (<= 0 start end size)
    (if (== start end)
      empty-vector-constant
      (let [^Tuple2 tup1 (.split v end)
            ^RrbTree8$ImRrbt before-end (._1 tup1)
            ^Tuple2 tup2 (.split before-end start)]
        (._2 tup2)))
    (throw (IndexOutOfBoundsException.))))

(defn subvec
  ([vec start]
   (subvec vec start (count vec)))
  ([^PaguroRrbVector vec start end]
   (PaguroRrbVector. (subvec* (.v vec) start end (count vec))
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

(let [^java.lang.reflect.Method m (.getDeclaredMethod RrbTree8$ImRrbt
                                                      "debugValidate"
                                                      (make-array Class 0))
      empty-object-arr (object-array [])]
  (.setAccessible m true)
  (defn check-invariants
    "Return a map that always contains the key :error with a boolean
    value of true if a problem was found in the data structure vec,
    otherwise false.  If true, there will also be a key :description
    with a string value, and :exception with a value of the exception
    thrown by the RrbTree8 class's debugValidate method."
    [^PaguroRrbVector vec]
    (let [^RrbTree8$ImRrbt v (.v vec)]
      (try
        (.invoke m v empty-object-arr)
        {:error false}
        (catch IllegalStateException e
          {:error true,
           :description (.getMessage e)
           :exception e})))))

(defn height [^PaguroRrbVector vec]
  (let [^RrbTree8$ImRrbt v (.v vec)]
    (.height v)))

(defn max-supported-height []
  ;; I believe 11 might be the maximum supported depth for Paguro
  ;; RrbTree, but not sure.  I will try that out and see if any tests
  ;; causes that height to be exceeded.
  11)

(defn lg-fullness
  "Return a positive value if the number of elements is considered
  'large enough' for a tree of the given height, or negative if it is
  is considered too low.  I am picking too low to be less than
  1/(2^(2*lg-max-branch-factor)) of maximum capacity for the given
  height.  It is already somewhat questionable to be less than
  1/(2^lg-max-branch-factor), but at least for now I'll give such
  trees a pass."
  [vec]
  (let [lg-max-branch-factor node-length-pow-2
        max-branch-factor strict-node-length
        h (height vec)
        lg-max-tree-capacity (* lg-max-branch-factor h)
        n (count vec)
        n (cond (zero? n) 1  ; special case to avoid -Infinity ret value
                (< n max-branch-factor) n
                :else (* max-branch-factor (quot n max-branch-factor)))
        lg-n (/ (Math/log n) (Math/log 2))]
    (+ (* 2 lg-max-branch-factor) (- lg-n lg-max-tree-capacity))))

(defn print-library-info []
  (println "\n" "NODE_LENGTH_POW_2" node-length-pow-2
           "\n" "STRICT_NODE_LENGTH" strict-node-length
           "\n" "HALF_STRICT_NODE_LENGTH" half-strict-node-length
           "\n" "MIN_NODE_LENGTH" min-node-length
           "\n" "MAX_NODE_LENGTH" max-node-length))


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
