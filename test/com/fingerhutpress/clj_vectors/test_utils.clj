(ns com.fingerhutpress.clj-vectors.test-utils
  (:require [clojure.test :as test]
            [clojure.string :as str]))

(def extra-checks? false)

(defn now-msec []
  (System/currentTimeMillis))

(def num-deftests-started (atom 0))
(def last-deftest-start-time (atom nil))

(defn print-jvm-classpath []
  (let [cp-str (System/getProperty "java.class.path")
        cp-strs (str/split cp-str #":")]
    (println "java.class.path:")
    (doseq [cp-str cp-strs]
      (println "  " cp-str))))

(defn print-test-env-info []
  (println "extra-checks?=" extra-checks?)
  (let [p (System/getProperties)]
    (println "java.vm.name" (get p "java.vm.name"))
    (println "java.vm.version" (get p "java.vm.version"))
    (print-jvm-classpath)
    (println "(clojure-version)" (clojure-version))))

(defmethod test/report :begin-test-var
  [m]
  (let [n (swap! num-deftests-started inc)]
    (when (== n 1)
      (print-test-env-info)))
  (println)
  (println "starting clj test" (:var m))
  (reset! last-deftest-start-time (now-msec)))

(defmethod test/report :end-test-var
  [m]
  (println "elapsed time (sec)" (/ (- (now-msec) @last-deftest-start-time)
                                   1000.0)))

;; Enable tests to be run on versions of Clojure before 1.10, when
;; ex-message was added.

(defn ex-message-copy
  "Returns the message attached to ex if ex is a Throwable.
  Otherwise returns nil."
  {:added "1.10"}
  [ex]
  (when (instance? Throwable ex)
    (.getMessage ^Throwable ex)))

(defn ex-cause-copy
  "Returns the cause of ex if ex is a Throwable.
  Otherwise returns nil."
  {:added "1.10"}
  [ex]
  (when (instance? Throwable ex)
    (.getCause ^Throwable ex)))
