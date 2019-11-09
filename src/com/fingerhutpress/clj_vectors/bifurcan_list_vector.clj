(ns com.fingerhutpress.clj-vectors.bifurcan-list-vector
  (:refer-clojure :exclude [vector vec subvec])
  (:require [com.fingerhutpress.clj-vectors.cljify-jvm-vector :as c]
            [com.fingerhutpress.clj-vectors.utils :as utils])
  (:import
   [io.lacuna.bifurcan
    List
    IList]
   [io.lacuna.bifurcan.nodes
    ListNodes$Node]))

(set! *warn-on-reflection* true)

(def listnodes-class io.lacuna.bifurcan.nodes.ListNodes)
(def shift-increment
  (utils/get-static-int-field listnodes-class "SHIFT_INCREMENT"))
(def max-branches
  (utils/get-static-int-field listnodes-class "MAX_BRANCHES"))

(def list-class io.lacuna.bifurcan.List)
(def ^java.lang.reflect.Field list-root-field
  (.getDeclaredField io.lacuna.bifurcan.List "root"))
(.setAccessible list-root-field true)

(def empty-vector-constant (List.))

(defn- empty-vec []
  empty-vector-constant)

(defn construct-list [^IList l vs]
  (let [l (.linear l)]
    (doseq [v vs]
      (.addLast l v))
    (.forked l)))

(defn- java-hashcode [^List v]
  (.hashCode v))

(defn- java-tostring [^List v]
  (.toString v))

(defn- get-nth [^List v idx]
  (.nth v idx))

(defn- append-one-elem [^List v elem]
  (.addLast v elem))

(defn- remove-last-elem [^List v]
  (.removeLast v))

(defn- replace-elem [^List v idx new-elem]
  (let [i (long idx)]
    (.set v i new-elem)))

(c/generate-vector-type BifurcanListVector
                        BifurcanListVecSeq
                        empty-vec
                        java-hashcode
                        java-tostring
                        get-nth
                        append-one-elem
                        remove-last-elem
                        replace-elem)

(defn vector [& elems]
  (let [v (BifurcanListVector. empty-vector-constant 0 nil 0 0)]
    (reduce conj v elems)))

(defn vec [coll]
  (apply vector coll))

(defn- subvec* [^List v start end size]
  (let [s (long start)
        e (long end)]
    (if (<= 0 s e size)
      (if (== s e)
        empty-vector-constant
        (.slice v s e))
      (throw (IndexOutOfBoundsException.)))))

(defn subvec
  ([vec start]
   (subvec vec start (count vec)))
  ([^BifurcanListVector vec start end]
   (BifurcanListVector. (subvec* (.v vec) start end (count vec))
                        (- end start) (meta vec) 0 0)))

(defn- splicev [^BifurcanListVector v1 ^BifurcanListVector v2]
  (assert (instance? BifurcanListVector v1))
  (assert (instance? BifurcanListVector v2))
  (when-not (instance? List (.v v1))
    (println "----------------------------------------------------------------------")
    (println "(class (.v v1))=" (class (.v v1)))
    (flush)
    (assert (instance? List (.v v1))))
  (when-not (instance? List (.v v2))
    (println "----------------------------------------------------------------------")
    (println "(class (.v v2))=" (class (.v v2)))
    (flush)
    (assert (instance? List (.v v2))))
  (let [^List bv1 (.v v1)
        ^List bv2 (.v v2)
        c (.concat bv1 bv2)]
    (assert (instance? List c))
    (BifurcanListVector. c (+ (count v1) (count v2))
                         ;; TBD: Not clear which metadata should be on
                         ;; returned vector, if any.  Perhaps nil
                         ;; would be more appropriate.
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

(defn check-invariants
  "Return a map that always contains the key :error with a boolean
  value of true if a problem was found in the data structure vec,
  otherwise false.  If true, there will also be a key :description
  with a string value."
  [^BifurcanListVector vec]
  (let [^List v (.v vec)]
    ;; TBD: Find or consider writing some code that checks invariants
    ;; about the List data structure v.
    {:error false}))

(defn height [^BifurcanListVector vec]
  (let [^List v (.v vec)
        ^ListNodes$Node root (.get list-root-field v)]
    (if (nil? root)
      1
      (let [s (.shift root)]
        (assert (zero? (mod s shift-increment)))
        (inc (quot s shift-increment))))))

(defn max-supported-height []
  ;; I believe 6 might be the maximum supported depth for bifurcan
  ;; List tree, but not sure.  I will try that out and see if any
  ;; tests causes that height to be exceeded.
  6)

(defn lg-fullness
  "Return a positive value if the number of elements is considered
  'large enough' for a tree of the given height, or negative if it is
  is considered too low.  I am picking too low to be less than
  1/(2^(2*lg-max-branch-factor)) of maximum capacity for the given
  height.  It is already somewhat questionable to be less than
  1/(2^lg-max-branch-factor), but at least for now I'll give such
  trees a pass."
  [vec]
  (let [lg-max-branch-factor shift-increment
        max-branch-factor max-branches
        h (height vec)
        lg-max-tree-capacity (* lg-max-branch-factor h)
        n (count vec)
        n (cond (zero? n) 1  ; special case to avoid -Infinity ret value
                (< n max-branch-factor) n
                :else (* max-branch-factor (quot n max-branch-factor)))
        lg-n (/ (Math/log n) (Math/log 2))]
    (+ (* 2 lg-max-branch-factor) (- lg-n lg-max-tree-capacity))))

(defn print-library-info []
  (println "\n" "SHIFT_INCREMENT" shift-increment
           "\n" "MAX_BRANCHES" max-branches))


(comment

(require '[com.fingerhutpress.clj-vectors.bifurcan-list-vector :as bv])
(import '(io.lacuna.bifurcan
          List
          IList)
        '(io.lacuna.bifurcan.nodes
          ListNodes$Node))

(def v0 (RrbTree/empty))
(def v1 (RrbTree$ImRrbt/empty))
(def v2 (RrbTree$MutableRrbt/empty))

(class v0)
;; org.organicdesign.fp.collections.RrbTree$ImRrbt
(= (class v0) (class v1) (class v2))
;; true

(def v10 (construct-list (empty-vec) (range 10)))
v10
(.hashCode v10)
;; TBD: why is this different than .hashCode of a Clojure vector of 10
;; elems?  Maybe because bifurcan List calls Java .hashCode on each
;; element, but Clojure vectors call a different method on each
;; element to calculate its hash?
(.nth v10 10)
(.nth v10 9)
(.hashCode (clojure.core/vec (range 10)))
(.toString v10)

)
