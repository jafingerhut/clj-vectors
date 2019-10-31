(ns com.fingerhutpress.clj-vectors.utils)


(defn same-coll? [a b]
  (and (= (count a)
          (count b)
          (.size ^java.util.Collection a)
          (.size ^java.util.Collection b))
       (= a b)
       (= b a)
       (= (hash a) (hash b))
       (= (.hashCode ^Object a) (.hashCode ^Object b))))


(defn slow-into [to from]
  (reduce conj to from))


(defn first-diff
  "Compare two sequences to see if they have = elements in the same
  order, and both sequences have the same number of elements.  If all
  of those conditions are true, and no exceptions occur while calling
  seq, first, and next on the seqs of xs and ys, then return -1.

  If two elements at the same index in each sequence are found not =
  to each other, or the sequences differ in their number of elements,
  return the index, 0 or larger, at which the first difference occurs.

  If an exception occurs while calling seq, first, or next, throw an
  exception that contains the index at which this exception occurred."
  [xs ys]
  (loop [i 0 xs (seq xs) ys (seq ys)]
    (if (try (and xs ys (= (first xs) (first ys)))
             (catch Exception e
               (.printStackTrace e)
               i))
      (let [xs (try (next xs)
                    (catch Exception e
                      (prn :xs i)
                      (throw e)))
            ys (try (next ys)
                    (catch Exception e
                      (prn :ys i)
                      (throw e)))]
        (recur (inc i) xs ys))
      (if (or xs ys)
        i
        -1))))


(defn check-subvec
  "Perform a sequence of calls to (test-subvec v start end) on a
  vector of a type under test, instances of which can be constructed
  from sequences via (seq->vec some-sequence).  Make corresponding
  calls on a normal Clojure vector, to compare the results.

  Return true if they give the same results, according to (same-coll?
  clojure-vector-result other-vector-type-result), otherwise false."
  [seq->vec test-subvec same-coll? init & starts-and-ends]
  (let [v1 (loop [v   (clojure.core/vec (range init))
                  ses (seq starts-and-ends)]
             (if ses
               (let [[s e] ses]
                 (recur (clojure.core/subvec v s e) (nnext ses)))
               v))
        v2 (loop [v   (seq->vec (range init))
                  ses (seq starts-and-ends)]
             (if ses
               (let [[s e] ses]
                 (recur (test-subvec v s e) (nnext ses)))
               v))]
    (same-coll? v1 v2)))

(defn check-catvec
  "Perform a sequence of calls to (test-catvec v start end) on a
  vector of a type under test, instances of which can be constructed
  from sequences via (seq->vec some-sequence).  Make corresponding
  calls on normal Clojure sequences, to compare the results.

  Return true if they give the same results, according to (same-coll?
  clojure-vector-result other-vector-type-result), otherwise false."
  [seq->vec test-catvec same-coll? & counts]
  (let [prefix-sums (reductions + counts)
        ranges (map range (cons 0 prefix-sums) prefix-sums)
        v1 (apply concat ranges)
        v2 (apply test-catvec (map seq->vec ranges))]
    (same-coll? v1 v2)))

(defn generative-check-subvec
  "Perform many calls to check-subvec with randomly generated inputs.
  Intended for use in tests of an alternate Clojure vector
  implementation.  Returns true if all tests pass, otherwise throws an
  exception containing data about the inputs that caused the failing
  test.

  See docs for check-subvec for the meaning of the first 3 arguments."
  [seq->vec test-subvec same-coll? iterations max-init-cnt slices]
  (dotimes [_ iterations]
    (let [init-cnt (rand-int (inc max-init-cnt))
          s1       (rand-int init-cnt)
          e1       (+ s1 (rand-int (- init-cnt s1)))]
      (loop [s&es [s1 e1] cnt (- e1 s1) slices slices]
        (if (or (zero? cnt) (zero? slices))
          (if-not (try (apply check-subvec seq->vec test-subvec same-coll?
                              init-cnt s&es)
                       (catch Exception e
                         (throw
                          (ex-info "check-subvec failure w/ Exception"
                                   {:init-cnt init-cnt :s&es s&es}
                                   e))))
            (throw
             (ex-info "check-subvec failure w/o Exception"
                      {:init-cnt init-cnt :s&es s&es})))
          (let [s (rand-int cnt)
                e (+ s (rand-int (- cnt s)))
                c (- e s)]
            (recur (conj s&es s e) c (dec slices)))))))
  true)

(defn generative-check-catvec
  "Perform many calls to check-catvec with randomly generated inputs.
  Intended for use in tests of an alternate Clojure vector
  implementation.  Returns true if all tests pass, otherwise throws an
  exception containing data about the inputs that caused the failing
  test.

  See docs for check-subvec for the meaning of the first 3 arguments."
  [seq->vec test-catvec same-coll? iterations max-vcnt min-cnt max-cnt]
  (dotimes [_ iterations]
    (let [vcnt (inc (rand-int (dec max-vcnt)))
          cnts (vec (repeatedly vcnt
                                #(+ min-cnt
                                    (rand-int (- (inc max-cnt) min-cnt)))))]
      (if-not (try (apply check-catvec seq->vec test-catvec same-coll? cnts)
                   (catch Exception e
                     (throw
                      (ex-info "check-catvec failure w/ Exception"
                               {:cnts cnts}
                               e))))
        (throw
         (ex-info "check-catvec failure w/o Exception" {:cnts cnts})))))
  true)
