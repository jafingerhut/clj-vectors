(ns com.fingerhutpress.clj-vectors.cljify-jvm-vector
  (:require [clojure.core.protocols :refer [IKVReduce]])
  (:import (clojure.lang RT Util)))


(set! *warn-on-reflection* true)


(defn throw-unsupported []
  (throw (UnsupportedOperationException.)))


;; For the data structure that you want to 'wrap' using this code, you
;; must provide functions that implement the following operations.
;; Example Java method calls are given for the Paguro RrbTree$ImRrrbt
;; class.

;; (empty-vec) - return an empty vector, e.g. RrbTree/empty
;; (java-hashcode v) - return Java hashCode for vector v,
;;   e.g. .hashCode
;; (java-tostring v) - return string representation, e.g. .toString
;; (get-nth v idx) - return vector v element at index idx, where first
;;   element is index 0.  e.g. (.get v i)
;; (append-one-elem v elem) - like clojure.core/conj on vector.
;;   e.g. .append
;; (remove-last-elem v) - like clojure.core/pop on vector.  See
;;   .without, but note that Paguro's without is more general than
;;   remove-last-elem / pop
;; (replace-elem v idx new-elem) - like clojure.core/assoc on vector.
;;   e.g. .replace

;; TBD: Consider allowing caller to provide a function that returns a
;; vector of elements provided in some other seq'able collection.  The
;; default implementation if no such thing was provided would
;; be (empty-vec) followed by N calls to append-one-elem.

;; These need not be provided.  This code implements them:
;; Object.equals

(defmacro generate-vector-type [vector-type-name-symbol
                                vector-seq-type-name-symbol
                                empty-vector
                                java-hashcode
                                java-tostring
                                get-nth
                                append-one-elem
                                remove-last-elem
                                replace-elem]
  `(do
(deftype ~vector-seq-type-name-symbol
    [~'vec ~'idx
     ~(with-meta '_meta   {:tag clojure.lang.IPersistentMap})
     ~(with-meta '_hash   {:unsynchronized-mutable true, :tag 'int})
     ~(with-meta '_hasheq {:unsynchronized-mutable true, :tag 'int})]

  Object
  (~'hashCode [~'this]
    (let [h# ~'_hash]
      (if (== h# 0)
        (loop [h# (int 1) xs# (seq ~'this)]
          (if xs#
            (let [x# (first xs#)]
              (recur (unchecked-add-int (unchecked-multiply-int (int 31) h#)
                                        (Util/hash x#))
                     (next xs#)))
            (do (set! ~'_hash (int h#))
                h#)))
        h#)))

  clojure.lang.IHashEq
  (~'hasheq [~'this]
    (let [h# ~'_hasheq]
      (if (== h# 0)
        (let [h# (hash-ordered-coll ~'this)]
          (do (set! ~'_hasheq (int h#))
              h#))
        h#)))

  clojure.lang.IMeta
  (~'meta [~'_] ~'_meta)

  clojure.lang.IObj
  (~'withMeta [~'_ ~'m]
    (new ~vector-seq-type-name-symbol ~'vec ~'idx ~'_meta ~'_hash ~'_hasheq))

  clojure.lang.Counted
  (~'count [~'this]
    (- (count ~'vec) ~'idx))

  clojure.lang.ISeq
  (~'first [~'_] (nth ~'vec ~'idx))
  (~'next [~'this]
    (let [nextidx# (inc ~'idx)]
      (when (< nextidx# (count ~'vec))
        (new ~vector-seq-type-name-symbol ~'vec nextidx# nil 0 0))))
  (~'more [~'this]
    (let [s# (.next ~'this)]
      (or s# (clojure.lang.PersistentList/EMPTY))))
  (~'cons [~'this ~'o]
    (clojure.lang.Cons. ~'o ~'this))
  (~'equiv [~'this ~'o]
    (cond
     (identical? ~'this ~'o) true
     (or (instance? clojure.lang.Sequential ~'o) (instance? java.util.List ~'o))
     (loop [me# ~'this
            you# (seq ~'o)]
       (if (nil? me#)
         (nil? you#)
         (and (Util/equiv (first me#) (first you#))
              (recur (next me#) (next you#)))))
     :else false))

  (~'empty [~'_]
    clojure.lang.PersistentList/EMPTY)

  clojure.lang.Seqable
  (~'seq [~'this] ~'this)

  clojure.lang.Sequential

  java.lang.Iterable
  (~'iterator [~'this]
    (let [xs# (clojure.lang.Box. (seq ~'this))]
      (reify java.util.Iterator
        (~'next [~'this]
          (locking xs#
            (if-let [v# (.-val xs#)]
              (let [x# (first v#)]
                (set! (.-val xs#) (next v#))
                x#)
              (throw
                (java.util.NoSuchElementException.
                  "no more elements in VecSeq iterator")))))
        (~'hasNext [~'this]
          (locking xs#
            (not (nil? (.-val xs#)))))
        (~'remove [~'this]
          (throw-unsupported))))))

(deftype ~vector-type-name-symbol
    [~'v
     ~(with-meta 'cnt     {:tag 'int})
     ~(with-meta '_meta   {:tag clojure.lang.IPersistentMap})
     ~(with-meta '_hash   {:unsynchronized-mutable true, :tag 'int})
     ~(with-meta '_hasheq {:unsynchronized-mutable true, :tag 'int})]
  Object
  (~'equals [~'this ~'that]
    (cond
      (identical? ~'this ~'that) true

      (or (instance? clojure.lang.IPersistentVector ~'that)
          (instance? java.util.RandomAccess ~'that))
      (and (== ~'cnt (count ~'that))
           (loop [i# (int 0)]
             (cond
               (== i# ~'cnt) true
               (.equals (.nth ~'this i#) (nth ~'that i#)) (recur (unchecked-inc-int i#))
               :else false)))
      
      (or (instance? clojure.lang.Sequential ~'that)
          (instance? java.util.List ~'that))
      (.equals (seq ~'this) (seq ~'that))

      :else false))

  (~'hashCode [~'this]
    (let [h# ~'_hash]
      (if (== h# 0)
        (let [h# (~java-hashcode ~'v)]
          (do (set! ~'_hash (int h#))
              h#))
        h#)))

  (~'toString [~'this]
    (~java-tostring ~'v))

  clojure.lang.IHashEq
  (~'hasheq [~'this]
    (let [h# ~'_hasheq]
      (if (== h# 0)
        (let [h# (hash-ordered-coll ~'this)]
          (do (set! ~'_hasheq (int h#))
              h#))
        h#)))

  clojure.lang.Counted
  (~'count [~'_] ~'cnt)

  clojure.lang.IMeta
  (~'meta [~'_] ~'_meta)

  clojure.lang.IObj
  (~'withMeta [~'_ ~'m]
    (new ~vector-type-name-symbol ~'v ~'cnt ~'m ~'_hash ~'_hasheq))

  clojure.lang.Indexed
  (~'nth [~'this ~'i]
    (~get-nth ~'v ~'i))

  (~'nth [~'this ~'i ~'not-found]
    (if (and (>= ~'i 0) (< ~'i ~'cnt))
      (.nth ~'this ~'i)
      ~'not-found))

  clojure.lang.IPersistentCollection
  (~'cons [~'this ~'val]
    (new ~vector-type-name-symbol (~append-one-elem ~'v ~'val)
         (unchecked-inc-int ~'cnt) ~'_meta 0 0))

  (~'empty [~'_]
    (new ~vector-type-name-symbol (~empty-vector) 0 ~'_meta 0 0))

  (~'equiv [~'this ~'that]
    (cond
      (or (instance? clojure.lang.IPersistentVector ~'that)
          (instance? java.util.RandomAccess ~'that))
      (and (== ~'cnt (count ~'that))
           (loop [i# 0]
             (cond
               (== i# ~'cnt) true
               (= (.nth ~'this i#) (nth ~'that i#)) (recur (unchecked-inc i#))
               :else false)))
        
      (or (instance? clojure.lang.Sequential ~'that)
          (instance? java.util.List ~'that))
      (Util/equiv (seq ~'this) (seq ~'that))
      
      :else false))

  clojure.lang.IPersistentStack
  (~'peek [~'this]
    (when (pos? ~'cnt)
      (.nth ~'this (dec ~'cnt))))

  (~'pop [~'this]
    (if (zero? ~'cnt)
      (throw (IllegalStateException. "Can't pop empty vector"))
      (new ~vector-type-name-symbol (~remove-last-elem ~'v)
           (unchecked-dec-int ~'cnt) ~'_meta 0 0)))

  clojure.lang.IPersistentVector
  (~'assocN [~'this ~'i ~'val]
    (let [i# (int ~'i)]
      (cond
        (and (<= 0 i#) (< i# ~'cnt))
        (new ~vector-type-name-symbol (~replace-elem ~'v i# ~'val) ~'cnt
             ~'_meta 0 0)

        (== i# ~'cnt) (.cons ~'this ~'val)
        :else (throw (IndexOutOfBoundsException.)))))

  (~'length [~'this]
    ~'cnt)

  clojure.lang.Reversible
  (~'rseq [~'this]
    (if (pos? ~'cnt)
      (clojure.lang.APersistentVector$RSeq. ~'this (unchecked-dec-int ~'cnt))
      nil))

  clojure.lang.Associative
  (~'assoc [~'this ~'k ~'val]
    (if (Util/isInteger ~'k)
      (.assocN ~'this ~'k ~'val)
      (throw (IllegalArgumentException. "Key must be integer"))))

  (~'containsKey [~'this ~'k]
    (and (Util/isInteger ~'k)
         (<= 0 (int ~'k))
         (< ~'k ~'cnt)))

  (~'entryAt [~'this ~'k]
    (if (.containsKey ~'this ~'k)
      (clojure.lang.MapEntry. ~'k (.nth ~'this ~'k))
      nil))

  clojure.lang.ILookup
  (~'valAt [~'this ~'i ~'not-found]
    (if (Util/isInteger ~'i)
      (if (and (>= ~'i 0) (< ~'i ~'cnt))
        (.nth ~'this ~'i)
        ~'not-found)
      ~'not-found))

  (~'valAt [~'this ~'k]
    (.valAt ~'this ~'k nil))

  clojure.lang.IFn
  (~'invoke [~'this ~'k]
    (if (Util/isInteger ~'k)
      (let [i# (int ~'k)]
        (if (and (>= i# (int 0)) (< i# ~'cnt))
          (.nth ~'this i#)
          (throw (IndexOutOfBoundsException.))))
      (throw (IllegalArgumentException. "Key must be integer"))))

  (~'applyTo [~'this ~'args]
    (let [n# (RT/boundedLength ~'args 1)]
      (case n#
        0 (throw (clojure.lang.ArityException.
                  n# (.. ~'this (~'getClass) (~'getSimpleName))))
        1 (.invoke ~'this (first ~'args))
        2 (throw (clojure.lang.ArityException.
                  n# (.. ~'this (~'getClass) (~'getSimpleName)))))))

  clojure.lang.Seqable
  (~'seq [~'this]
    (if (zero? ~'cnt)
      nil
      (new ~vector-seq-type-name-symbol ~'this 0 nil 0 0)))

  clojure.lang.Sequential

  java.lang.Comparable
  (~'compareTo [~'this ~'that]
    (if (identical? ~'this ~'that)
      0
      (let [^clojure.lang.IPersistentVector vthat#
            (cast clojure.lang.IPersistentVector ~'that)
            vcnt# (.count vthat#)]
        (cond
          (< ~'cnt vcnt#) -1
          (> ~'cnt vcnt#) 1
          :else
          (loop [i# (int 0)]
            (if (== i# ~'cnt)
              0
              (let [comp# (Util/compare (.nth ~'this i#) (.nth vthat# i#))]
                (if (zero? comp#)
                  (recur (unchecked-inc-int i#))
                  comp#))))))))

  ;; core.rrb-vector has a more efficient implementation of kv-reduce
  ;; that is customized for its data structure.  Here we are not going
  ;; to make any assumptions about the data structure used to
  ;; implement the vector, so this implementation is likely not the
  ;; most efficient one.
  IKVReduce
  (~'kv-reduce [~'this ~'f ~'init]
    (loop [i# (int 0)
           init# ~'init]
      (let [nextinit# (~'f init# i# (.nth ~'this i#))]
        (if (reduced? nextinit#)
          @nextinit#
          (let [nexti# (unchecked-inc-int i#)]
            (if (< nexti# ~'cnt)
              (recur nexti# nextinit#)
              nextinit#))))))

  java.lang.Iterable
  (~'iterator [~'this]
    (let [i# (java.util.concurrent.atomic.AtomicInteger. 0)]
      (reify java.util.Iterator
        (~'hasNext [~'_] (< (.get i#) ~'cnt))
        (~'next [~'_]
          (try
            (.nth ~'this (unchecked-dec-int (.incrementAndGet i#)))
            (catch IndexOutOfBoundsException e#
              (throw (java.util.NoSuchElementException.
                       "no more elements in RRB vector iterator")))))
        (~'remove [~'_] (throw-unsupported)))))

  java.util.Collection
  (~'contains [~'this ~'o]
    (boolean (some #(= % ~'o) ~'this)))

  (~'containsAll [~'this ~'c]
    (every? #(.contains ~'this %) ~'c))

  (~'isEmpty [~'this]
    (zero? (count ~'this)))

  (~'toArray [~'this]
    (into-array Object ~'this))

  (~(with-meta 'toArray {:tag "[Ljava.lang.Object;"})
   [~'this ~(with-meta 'arr {:tag "[Ljava.lang.Object;"})]
   (if (>= (count ~'arr) ~'cnt)
     (do (dotimes [i# ~'cnt]
           (aset ~'arr i# (nth ~'v i#)))
         ~'arr)
     (into-array Object ~'this)))

  (~'size [~'this]
    (count ~'this))

  (~'add [~'_ ~'o]             (throw-unsupported))
  (~'addAll [~'_ ~'c]          (throw-unsupported))
  (~'clear [~'_]               (throw-unsupported))
  (~(with-meta 'remove {:tag 'boolean}) [~'_ ~'o] (throw-unsupported))
  (~'removeAll [~'_ ~'c]       (throw-unsupported))
  (~'retainAll [~'_ ~'c]       (throw-unsupported))

  java.util.List
  (~'get [~'this ~'i] (.nth ~'this ~'i))

  (~'indexOf [~'this ~'o]
    (loop [i# (int 0)]
      (cond
        (== i# ~'cnt) -1
        (= ~'o (.nth ~'this i#)) i#
        :else (recur (unchecked-inc-int i#)))))

  (~'lastIndexOf [~'this ~'o]
    (loop [i# (unchecked-dec-int ~'cnt)]
      (cond
        (neg? i#) -1
        (= ~'o (.nth ~'this i#)) i#
        :else (recur (unchecked-dec-int i#)))))

  (~'listIterator [~'this]
    (.listIterator ~'this 0))

  (~'listIterator [~'this ~'i]
    (let [i# (java.util.concurrent.atomic.AtomicInteger. ~'i)]
      (reify java.util.ListIterator
        (~'hasNext [~'_] (< (.get i#) ~'cnt))
        (~'hasPrevious [~'_] (pos? i#))
        (~'next [~'_]
          (try
            (.nth ~'this (unchecked-dec-int (.incrementAndGet i#)))
            (catch IndexOutOfBoundsException e#
              (throw (java.util.NoSuchElementException.
                       "no more elements in RRB vector list iterator")))))
        (~'nextIndex [~'_] (.get i#))
        (~'previous [~'_] (.nth ~'this (.decrementAndGet i#)))
        (~'previousIndex [~'_] (unchecked-dec-int (.get i#)))
        (~'add [~'_ ~'e]  (throw-unsupported))
        (~'remove [~'_]   (throw-unsupported))
        (~'set [~'_ ~'e]  (throw-unsupported)))))

  (~'subList [~'this ~'a ~'z]      (throw-unsupported))
  (~'add [~'_ ~'i ~'o]             (throw-unsupported))
  (~'addAll [~'_ ~'i ~'c]          (throw-unsupported))
  (~(with-meta 'remove {:tag Object}) [~'_ ~(with-meta 'i {:tag 'int})] (throw-unsupported))
  (~'set [~'_ ~'i ~'e]             (throw-unsupported)))

))
