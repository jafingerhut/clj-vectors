(ns com.fingerhutpress.clj-vectors.test-long
  (:require [clojure.test :as test :refer [deftest testing is are]]
            [com.fingerhutpress.clj-vectors.utils :as utils])
  (:import (clojure.lang ExceptionInfo)))

(def generative-test-length :short)

(def check-subvec-params (case generative-test-length
                           :short  [125 100000 10]
                           :medium [250 200000 20]
                           :long   [250 200000 20]))

(defn test-slicing-generative
  [{:keys [generative-check-subvec] :as opts}]
  (testing "slicing (generative)"
    (is (try
          (apply generative-check-subvec check-subvec-params)
          (catch ExceptionInfo e
            (throw (ex-info (format "%s: %s %s"
                                    (ex-message e)
                                    (:init-cnt (ex-data e))
                                    (:s&es (ex-data e)))
                            {}
                            (ex-cause e))))))))

;; short: 2 to 3 sec
;; medium: 50 to 60 sec
(def check-catvec-params (case generative-test-length
                           :short  [ 10 30 10 60000]
                           :medium [250 30 10 60000]
                           :long   [250 30 10 60000]))

(defn test-splicing-generative
  [{:keys [generative-check-catvec] :as opts}]
  (testing "splicing (generative)"
    (is (try
          (apply generative-check-catvec check-catvec-params)
          (catch ExceptionInfo e
            (throw (ex-info (format "%s: %s"
                                    (ex-message e)
                                    (:cnts (ex-data e)))
                            {}
                            (ex-cause e))))))))


;; This problem reproduction code is from CRRBV-17 ticket:
;; https://clojure.atlassian.net/projects/CRRBV/issues/CRRBV-17

(def benchmark-size 100000)

;; This small variation of the program in the ticket simply does
;; progress debug printing occasionally, as well as extra debug
;; checking of the results occasionally.

;; If you enable the printing of the message that begins
;; with "splice-rrbts result had shift" in function
;; fallback-to-slow-splice-if-needed, then run this test, you will see
;; it called hundreds or perhaps thousands of times.  The fallback
;; approach is effective at avoiding a crash for this scenario, but at
;; a dramatic extra run-time cost.

(defn vector-push-f [v seq->vec test-catvec checking-catvec]
  (loop [v v
         i 0]
    (let [check? (or (zero? (mod i 10000))
                     (and (> i 99000) (zero? (mod i 100)))
                     (and (> i 99900) (zero? (mod i 10))))]
      (when check?
        (println "i=" i " ")
        #_(u/print-optimizer-counts))
      (if (< i benchmark-size)
        (recur (if check?
                 (checking-catvec (seq->vec [i]) v)
                 (test-catvec (seq->vec [i]) v))
               (inc i))
        v))))

;; Approximate run times for this test on a 2015 MacBook Pro
;;  36 sec - clj 1.10.1, OpenJDK 11.0.4
;; 465 sec - cljs 1.10.439, OpenJDK 11.0.4, Nashorn JS runtime
;; 138 sec - cljs 1.10.238, OpenJDK 11.0.4, nodejs 8.10.0
;; 137 sec - cljs 1.10.238, OpenJDK 11.0.4, Spidermonkey JavaScript-C52.9.1
(defn test-crrbv-17
  [{:keys [seq->vec test-catvec] :as opts}]
  #_(u/reset-optimizer-counts!)
  ;; TBD: Consider passing in, within opts, both test-catvec and
  ;; checking-catvec, for test functions like this where it is
  ;; beneficial to use both.
  (let [checking-catvec test-catvec]
    (is (= (reverse (range benchmark-size))
           (vector-push-f (seq->vec []) seq->vec test-catvec checking-catvec)))))


(defn test-all-long
  [opts]
  (assert (every? #(contains? opts %) [:seq->vec
                                       :test-subvec
                                       :test-catvec
                                       :same-coll?]))
  (let [{:keys [seq->vec test-subvec test-catvec same-coll?]} opts
        opts (assoc opts
                    :generative-check-subvec
                    (partial utils/generative-check-subvec
                             seq->vec test-subvec same-coll?)
                    :generative-check-catvec
                    (partial utils/generative-check-catvec
                             seq->vec test-catvec same-coll?))]
    (test-slicing-generative opts)
    (test-splicing-generative opts)
    (test-crrbv-17 opts)))
