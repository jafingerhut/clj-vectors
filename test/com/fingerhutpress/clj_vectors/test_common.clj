(ns com.fingerhutpress.clj-vectors.test-common
  (:require [clojure.test :as test :refer [testing is are]]
            [com.fingerhutpress.clj-vectors.utils :as utils]
            [com.fingerhutpress.clj-vectors.test-utils :as u]
            [clojure.core.reducers :as r]))

;; Tests surrounded by (when do-paguro-312-failing-tests ...) fail with
;; Paguro version 3.1.2.  See this PR and issue:
;; https://github.com/GlenKPeterson/Paguro/pull/33
;; https://github.com/GlenKPeterson/Paguro/issues/31

;; The proposed fix in the PR causes all of those tests to pass for
;; me, so it seems at least not completely wrong.
(def do-paguro-312-failing-tests true)


(defn test-slicing
  [{:keys [check-subvec] :as opts}]
  (is (check-subvec 32000 10 29999 1234 18048 10123 10191)))

(defn test-splicing
  [{:keys [check-catvec test-catvec seq->vec] :as opts}]
  (is (= true (check-catvec 1025 1025 3245 1025 32768 1025 1025 10123 1025 1025)))
  (when do-paguro-312-failing-tests
    (is (= true (check-catvec 10 40 40 40 40 40 40 40 40))))
  (is (= true (apply check-catvec (repeat 30 33))))
  (is (= true (check-catvec 26091 31388 1098 43443 46195 4484 48099 7905
                            13615 601 13878 250 10611 9271 53170)))
  
  ;; Order that catvec will perform splicev calls:
  (let [my-splice test-catvec
        counts [26091 31388 1098 43443 46195 4484 48099 7905
                13615 601 13878 250 10611 9271 53170]
        
        prefix-sums (reductions + counts)
        ranges (map range (cons 0 prefix-sums) prefix-sums)
        
        [v01 v02 v03 v04 v05 v06 v07 v08
         v09 v10 v11 v12 v13 v14 v15] (map seq->vec ranges)
        
        v01-02 (my-splice v01 v02)  ;; top level catvec call
        v03-04 (my-splice v03 v04)  ;; top level catvec call
        v01-04 (my-splice v01-02 v03-04)  ;; top level catvec call
        
        v05-06 (my-splice v05 v06)  ;; recurse level 1 catvec call
        v07-08 (my-splice v07 v08)  ;; recurse level 1 catvec call
        v05-08 (my-splice v05-06 v07-08)  ;; recurse level 1 catvec call
        
        v09-10 (my-splice v09 v10)  ;; recurse level 2 catvec call
        v11-12 (my-splice v11 v12)  ;; recurse level 2 catvec call
        v09-12 (my-splice v09-10 v11-12)  ;; recurse level 2 catvec call
        
        v13-14 (my-splice v13 v14)  ;; recurse level 3 catvec call
        v13-15 (my-splice v13-14 v15)  ;; recurse level 3 catvec call
        
        v09-15 (my-splice v09-12 v13-15)  ;; recurse level 2 catvec call
        
        v05-15 (my-splice v05-08 v09-15)  ;; recurse level 1 catvec call
        
        v01-15 (my-splice v01-04 v05-15)  ;; top level catvec call
        
        exp-val (range (last prefix-sums))]
    (is (= -1 (utils/first-diff v01-15 exp-val)))
    (when do-paguro-312-failing-tests
      (is (= -1 (utils/first-diff (into v01-04 v05-15) exp-val))))))

(defn test-reduce
  [{:keys [seq->vec] :as opts}]
  (let [v1 (clojure.core/vec (range 128))
        v2 (seq->vec (range 128))]
    (testing "reduce"
      (is (= (reduce + v1) (reduce + v2))))
    (testing "reduce-kv"
      (is (= (reduce-kv + 0 v1) (reduce-kv + 0 v2))))))

(defn test-reduce-2
  [{:keys [seq->vec test-subvec] :as opts}]
  (let [v1 (test-subvec (seq->vec (range 1003)) 500)
        v2 (seq->vec (range 500 1003))]
    (is (= (reduce + 0 v1)
           (reduce + 0 v2)
           (reduce + 0 (r/map identity (seq v1)))
           (reduce + 0 (r/map identity (seq v2)))))))

(defn test-seq
  [{:keys [seq->vec] :as opts}]
  (let [v (seq->vec (range 128))
        s (seq v)]
    (testing "seq contents"
      (is (= v s)))
    ;; This test passes for core.rrb-vector vectors, but it doesn't
    ;; seem useful to require that all vector implementations I want
    ;; to test return chunked sequences.
    #_(testing "chunked-seq?"
      (is (chunked-seq? s)))
    (testing "internal-reduce"
      (is (satisfies? clojure.core.protocols/InternalReduce
                      s)))))

(defn test-assoc
  [{:keys [seq->vec test-subvec] :as opts}]
  (let [v1 (seq->vec (range 40000))
        v2 (reduce (fn [out [k v]]
                     (assoc out k v))
                   (assoc v1 40000 :foo)
                   (map-indexed vector (rseq v1)))]
    (is (= (concat (rseq v1) [:foo]) v2)))
  (are [i] (= :foo
              (-> (range 40000)
                  (seq->vec)
                  (test-subvec i)
                  (assoc 10 :foo)
                  (nth 10)))
    1 32 1024 32768))

(defn test-assoc!
  [{:keys [seq->vec test-subvec] :as opts}]
  (let [v1 (seq->vec (range 40000))
        v2 (persistent!
            (reduce (fn [out [k v]]
                      (assoc! out k v))
                    (assoc! (transient v1) 40000 :foo)
                    (map-indexed vector (rseq v1))))]
    (is (= (concat (rseq v1) [:foo]) v2)))
  (are [i] (= :foo
              (-> (range 40000)
                  (seq->vec)
                  (test-subvec i)
                  (transient)
                  (assoc! 10 :foo)
                  (persistent!)
                  (nth 10)))
    1 32 1024 32768))

(defn test-relaxed
  [{:keys [seq->vec test-catvec] :as opts}]
  (when do-paguro-312-failing-tests
    (is (= (into (test-catvec (seq->vec (range 123)) (seq->vec (range 68)))
                 (range 64))
           (concat (range 123) (range 68) (range 64)))))
  (when do-paguro-312-failing-tests
    (is (= (utils/slow-into (test-catvec (seq->vec (range 123))
                                         (seq->vec (range 68)))
                            (range 64))
           (concat (range 123) (range 68) (range 64))))))

(defn test-hasheq
  [{:keys [empty-vector seq->vec test-catvec] :as opts}]
  (is (= (hash []) (hash (empty-vector))))  ;; CRRBV-25
  (let [v1 (seq->vec (range 1024))
        v2 (seq->vec (range 1024))
        v3 (test-catvec (seq->vec (range 512)) (seq->vec (range 512 1024)))
        s1 (seq v1)
        s2 (seq v2)
        s3 (seq v3)]
    (is (= (hash v1) (hash v2) (hash v3) (hash s1) (hash s2) (hash s3)))
    (is (= (hash (nthnext s1 120))
           (hash (nthnext s2 120))
           (hash (nthnext s3 120))))))

(defn test-reduce-subvec-catvec
  [{:keys [seq->vec test-catvec test-subvec] :as opts}]
  (letfn [(insert-by-sub-catvec [v n]
            (test-catvec (test-subvec v 0 n) (seq->vec ['x])
                         (test-subvec v n)))
          (repeated-subvec-catvec [i]
            (reduce insert-by-sub-catvec (seq->vec (range i)) (range i 0 -1)))]
    (is (= (repeated-subvec-catvec 2371)
           (interleave (range 2371) (repeat 'x))))))

(def pos-infinity Double/POSITIVE_INFINITY)

(defn test-reduce-subvec-catvec2
  [{:keys [seq->vec test-catvec test-subvec] :as opts}]
  (letfn [(insert-by-sub-catvec [v n]
            (test-catvec (test-subvec v 0 n) (seq->vec ['x])
                         (test-subvec v n)))
          (repeated-subvec-catvec [i]
            (reduce insert-by-sub-catvec
                    (seq->vec (range i))
                    (take i (interleave (range (quot i 2) pos-infinity)
                                        (range (quot i 2) pos-infinity)))))]
    (let [n 2371
          v (repeated-subvec-catvec n)]
      (is (every? #(or (integer? %) (= 'x %)) v))
      (is (= (count v) (* 2 n))))))

(defn test-splice-high-subtree-branch-count
  [{:keys [seq->vec test-catvec test-subvec] :as opts}]
  (let [x        (seq->vec (repeat 1145 \a))
        y        (test-catvec (test-subvec x 0 778)
                              (test-subvec x 778 779)
                              (seq->vec [1])
                              (test-subvec x 779))
        z        (test-catvec (test-subvec y 0 780)
                              (seq->vec [2])
                              (test-subvec y 780 781)
                              (test-subvec y 781))
        res      (test-catvec (test-subvec z 0 780)
                              (seq->vec [])
                              (seq->vec [3])
                              (test-subvec z 781))
        expected (concat (repeat 779 \a) [1] [3] (repeat 366 \a))]
    (is (= res expected))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; This problem reproduction code is from CRRBV-12 ticket:
;; https://clojure.atlassian.net/projects/CRRBV/issues/CRRBV-12

;; I would prefer to have all of the data that is the value of
;; crrbv-12-data read from a separate file, but it is not terribly
;; long, and having it in the code avoids having to figure out how to
;; find and read the file on N different JavaScript runtime
;; environments, for the ClojureScript version of the test.

(def crrbv-12-data
  [7912 7831 5393 5795 6588 2394 6403 6237 6152 5890 6507 6388 6100
  7400 6340 7624 6379 5430 6335 5883 5570 6220 6319 6442 5666 3901
  6974 5440 6626 7782 6760 6066 7763 9547 5585 6724 5407 5675 7727
  7666 6845 6658 5409 7304 7291 5826 6523 5529 7387 6275 7193 5563
  6572 7150 2949 1133 7312 7267 7135 7787 5812 7372 4295 5937 2931
  4846 6149 1901 6680 7319 7845 7517 6722 6535 6362 5457 6649 7757
  7463 6755 7436 6364 7361 7174 6048 6657 6533 5763 6074 6744 6734
  5668 61 3842 5395 6489 1723 6248 7664 6645 5943 5428 6995 6688 7088
  6305 6198 6197 5765 3691 7157 7305 7631 6058 6655 7846 7746 686 6024
  6473 6150 5951 1761 7900 7084 5637 6607 5561 5772 7232 8512 6249
  7377 5437 4830 6939 6355 7100 7884 951 6765 7054 1367 4580 7284 5414
  7344 7525 5801 6374 6685 6737 4413 7353 1851 5973 7538 7116 6359
  6605 6743 6153 7398 4757 6623 7546 7013 7091 7501 5749 6368 7911
  6675 3246 6304 6469 6868 7701 5768 6369 6996 6346 6171 5884 6757
  7615 5986 9904 5982 7049 6011 7716 6646 6178 6636 6637 7700 3390
  6107 6938 2513 5663 5309 5673 7069 6615 5825 7183 5600 2188 5807
  7635 7257 4803 6740 5865 6869 6968 7404 5124 7565 6169 7681 6181
  5427 9861 7669 5936 5588 5463 6059 5695 5784 6768 6922 5720 6229
  9173 6486 6399 6013 5517 7198 7320 6970 5969 7593 7351 7622 6561
  5739 6433 6452 6320 6979 6260 6763 5539 6292 7133 6571 6108 7455
  8470 7148 7597 6935 6865 7852 6549 6506 5425 6552 5551 5612 7230 809
  2694 6408 6783 7626 6703 2754 1015 6809 7584 5473 6165 7105 6447
  5856 6739 5564 7886 7856 7355 5814 919 6900 6257 118 7259 7419 6278
  7619 6401 5970 7537 2899 6012 7190 5500 6122 5817 7620 6402 5811
  5412 6822 5643 6138 5948 5523 4884 6460 5828 7159 5405 6224 7192
  8669 5827 538 7416 6598 5577 6769 7547 7323 6748 6398 1505 6211 6466
  6699 6207 6444 6863 7646 5917 6796 5619 6282 354 6418 5687 2536 6238
  1166 6376 3852 5955 188 7218 7477 6926 7694 7253 5880 5424 7392 6337
  7438 7814 3205 6336 6465 6812 1102 6468 6034 6133 5849 7578 7863
  5761 6372 7568 5813 6380 6481 6942 7676 5552 7015 7120 7838 5684
  6101 6834 6092 7917 6124 867 7187 5527 7488 5900 6267 6443 724 6073
  6608 6407 6040 5540 6061 5554 5469 6255 6542 7336 2272 6921 1078
  5593 7045 5013 6870 6712 6537 6785 6333 5892 6633 7522 6697 5915
  5567 6606 5820 7653 7554 6932 5824 9330 8780 7203 7204 7519 7633
  6529 7564 5718 7605 6579 7621 4462 6009 6950 6430 5911 5946 6877
  7830 6570 7421 6449 6684 8425 5983 5846 5505 6097 5773 5781 6463
  6867 5774 6601 1577 5642 6959 6251 7741 7391 6036 6892 5097 6874
  6580 6348 5904 6709 5976 7411 7223 6252 7414 6813 4378 5888 5546
  6385 401 5912 7828 7775 5925 6151 7648 5810 7673 6250 5808 7251 1407
  5644 7439 7901 1964 6631 6858 7630 7771 2892 946 6397 5443 5715 5665
  7306 6233 5566 5447 7011 6314 2054 5786 2170 6901 6077 6239 7791
  6960 7891 7878 7758 5829 7611 7059 5455 6654 6459 6949 7406 7854
  5805 6564 7033 6445 5939 6706 6103 7614 7902 6527 7479 6196 6484
  3521 7269 6055 7331 6184 6746 6936 5891 6687 5771 7136 6625 7865
  5864 6704 7726 5842 6295 6910 5277 7528 5689 5674 7457 7086 5220 317
  7720 6720 5913 7098 5450 7275 7521 7826 7007 6378 7277 6844 7177
  5482 97 6730 7861 5601 6000 6039 6953 5624 6450 6736 7492 5499 5822
  7276 2889 7102 6648 6291 865 7348 7330 1449 6719 5550 7326 6338 6714
  7805 7082 6377 2791 7876 5870 7107 7505 5416 7057 6021 7037 6331
  5698 6721 5180 7390 5938 9067 7215 4566 8051 6557 6161 5894 1379
  7335 2602 6520 7199 6878 6366 6948 7202 4791 7338 7442 5987 7099
  7632 5453 4755 4947 7786 6254 7103 7595 6670 6485 6117 6756 6339
  7240 7609 6853 6299 7205 4857 7511 576 5835 5396 5997 5508 6413 6219
  5403 7686 9189 6634 5503 6801 7508 5611 7667 7572 7587 6015 7153
  7340 6279 5646 2004 2708 7119 5737 3258 7427 6204 6476 6511 2300
  7055 5389 6984 5438 6002 6272 5756 5734 6913 6425 6847 5657 6357
  6862 6030 5522 6943 3518 6139 6671 7764 6493 5691 6082 4635 6640
  6898 7262 9391 6828 2277 6690 6464 5759 7441 6622 1262 7114 6294
  7070 6539 6788 6167 7824 6382 2512 7322 5992 7696 5445 5538 6140
  7151 6409 7085 6166 6263 1194 5544 7141 5906 2939 7389 7290 6491
  6322 8324 7341 7246 5610 7536 6946 7540 7760 6293 5589 7009 7822
  5456 6805 5841 7722 5559 7265 6903 3517 1243 6078 7180 6147 8063
  7395 7551 5460 6421 7567 6546 6941 6301 5486 7347 6479 5990 5932
  6881 7737 6051 7375 5762 6897 2967 7297 7263 6965 6752 6158 7556
  6794 7641 7628 2374 6289 7286 7581 6008 491 6919 9157 7002 6585 7960
  6967 7692 7128 5680 5037 5752 6223 5989 7545 6584 7282 6221 871 6116
  5484 6350 6266 6889 6216 1892 924 5875 7658 5461 5410 8352 7072 5724
  6931 6050 6125 5519 6711 7518 6613 7576 7989 5603 7214 6664 2933
  5839 7454 9353 6512 7242 7768 6037 6567 6673 8438 7364 5406 6080 577
  6895 5742 5722 6944 6273 5965 5464 6876 7719 7311 7258 6829 7280
  6028 5740 9162 9858 6695 7239 6972 7025 7147 7039 6226 6135 7219
  6477 6708 767 5432 7405 7580 3790 372 7523 6597 5922 6105 5434 9587
  6173 7739 5984 5854 2153 6912 7476 7598 5985 5874 8723 5628 5496
  7352 4829 6483 7211 6933 5545 7544 5444 5790 8223 1089 6676 5667
  6749 6777 5429 6347 5399 5662 6446 5524 6909 5415 7742 6343 5921
  7160 7175 7026 1838 6894 4355 52 6192 5341 6945 7366 7816 2006 7380
  6531 6904 5958 6270 6069 5574 7349 7212 5256 6010 6961 2825 6691
  7792 6017 6888 7707 6693 6456 5871 7238 7780 7256 5630 7744 6855
  5077 6958 6046 6707 6530 6501 7298 5636 6121 1105 6243 5541 6814
  6732 7500 6866 7093 7745 7030 4338 6517 5991 6458 6213 4695 5542
  7853 5926 6550 5230 7432 7006 5858 7677 6495 7310 6432 7487 7670
  7674 6245 7315 7893 4360 940 6303 5757 7697 7506 5491 1309 7695 2214
  5553 6964 7403 7302 6589 7851 7186 6193 2964 6242 6545 7012 7010
  5448 5767 6647 7610 7485 6509 6083 6525 5607 9982 6244 7832 7213
  6308 1320 7092 5656 6342 7864 7140 2577 104 1343 6786 7654 6156 5584
  6818 5604 6681 6038 6056 6594 6603 7040 5468 5957 7229 6735 5510
  6700 7725 7431 7154 7682 6558 7158 7470 7749 5400 5397 7247 6582
  5832 7041 7325 5777 6759 6577 6195 7895 9626 7042 6026 6741 7811
  7942 8926 1499 6772 7561 5565 3587 7273 6172 7428 6787 7181 5754
  7579 5535 5543 5818 7264 1854 6998 7425 5394 6661 6562 375 2990])

(defn- quicksort [test-catvec seq->vec v]
  (if (<= (count v) 1)
    v
    (let [[x & xs] v]
      (test-catvec (quicksort test-catvec seq->vec
                              (seq->vec (filter #(<= % x) xs)))
                   (seq->vec [x])
                   (quicksort test-catvec seq->vec
                              (seq->vec (filter #(> % x) xs)))))))

(defn- ascending? [coll]
  (every? (fn [[a b]] (<= a b))
          (partition 2 1 coll)))

(defn test-crrbv-12
  [{:keys [seq->vec test-catvec] :as opts}]
  (let [v (seq->vec crrbv-12-data)]
    (when do-paguro-312-failing-tests
      (testing "Ascending order after quicksort"
        (is (ascending? (quicksort test-catvec seq->vec v)))))
    (testing "Repeated catvec followed by pop"
      (is (= [] (nth (iterate pop
                              (nth (iterate #(test-catvec (seq->vec [0]) %)
                                            (seq->vec []))
                                   963))
                     963))))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn test-npe-for-1025-then-pop!
  [{:keys [array-kind empty-vector empty-vector-of test-pop!] :as opts}]
  (let [bfactor-squared (* 32 32)
        mk-empty-vector (case array-kind
                          :object-array empty-vector
                          :long-array #(empty-vector-of :long))
        boundary 54
        v1 (-> (mk-empty-vector)
               (into (range boundary))
               (into (range boundary (inc bfactor-squared))))
        v2 (-> (mk-empty-vector)
               (into (range bfactor-squared))
               (transient)
               (test-pop!)
               (persistent!))
        v3 (-> (mk-empty-vector)
               (into (range boundary))
               (into (range boundary (inc bfactor-squared)))
               (transient)
               (test-pop!)
               (persistent!))
        v4 (-> (mk-empty-vector)
               (into (range (inc bfactor-squared)))
               (transient)
               (test-pop!)
               (persistent!))]
    (is (= (seq v1) (range (inc bfactor-squared))))
    (is (= (seq v2) (range (dec bfactor-squared))))
    ;; This used to fail with core.rrb-vector version 0.0.14 with
    ;; NullPointerException while traversing the seq on clj.  It gets
    ;; a different kind of error with cljs.
    (is (= (seq v3) (range bfactor-squared)))
    ;; This one caused a NullPointerException with version 0.0.14
    ;; while traversing the seq
    (is (= (seq v4) (range bfactor-squared)))))

;; This problem reproduction code is slightly modified from a version
;; provided in a comment by Mike Fikes on 2018-Dec-09 for this issue:
;; https://clojure.atlassian.net/projects/CRRBV/issues/CRRBV-20

(defn- play [seq->vec test-catvec test-subvec players rounds]
  (letfn [(swap [marbles split-ndx]
            (test-catvec
             (test-subvec marbles split-ndx)
             (test-subvec marbles 0 split-ndx)))
          (rotl [marbles n]
            (swap marbles (mod n (count marbles))))
          (rotr [marbles n]
            (swap marbles (mod (- (count marbles) n) (count marbles))))
          (place-marble
            [marbles marble]
            (let [marbles (rotl marbles 2)]
              [(test-catvec (seq->vec [marble]) marbles) 0]))
          (remove-marble [marbles marble]
            (let [marbles (rotr marbles 7)
                  first-marble (nth marbles 0)]
              [(test-subvec marbles 1) (+ marble first-marble)]))
          (play-round [marbles round]
            (if (zero? (mod round 23))
              (remove-marble marbles round)
              (place-marble marbles round)))
          (add-score [scores player round-score]
            (if (zero? round-score)
              scores
              (assoc scores player (+ (get scores player 0) round-score))))]
    (loop [marbles (seq->vec [0])
           round   1
           player  1
           scores  {}
           ret     []]
      (let [[marbles round-score] (play-round marbles round)
            scores (add-score scores player round-score)]
        (if (> round rounds)
          (conj ret {:round round :marbles marbles})
          (recur marbles
                 (inc round)
                 (if (= player players) 1 (inc player))
                 scores
                 (conj ret {:round round :marbles marbles})))))))

(defn- play-core [& args]
  (apply play clojure.core/vec clojure.core/into clojure.core/subvec args))

;; 'vut' is 'vector under test'
(defn- play-vut
  [{:keys [seq->vec test-catvec test-subvec] :as opts}
   & args]
  (apply play seq->vec test-catvec test-subvec args))

(defn test-crrbv-20
  [{:keys [test-subvec] :as opts}]
  ;; This one passes with core.rrb-vector
  (when do-paguro-312-failing-tests
    (is (= (play-core 10 1128)
           (play-vut opts 10 1128))))
  ;; This ends up with (play-vut opts 10 1129) throwing an exception, with
  ;; core.rrb-vector version 0.0.14
  (when do-paguro-312-failing-tests
    (is (= (play-core 10 1129)
           (play-vut opts 10 1129))))

  ;; The previous test demonstrates a bug in the transient RRB vector
  ;; implementation.  The one below demonstrated a similar bug in the
  ;; persistent RRB vector implementation in version 0.0.14.
  (let [v1128 (:marbles (last (play-vut opts 10 1128)))
        v1129-pre (-> v1128
                      (test-subvec 2)
                      (conj 2001))]
    (is (every? integer? (conj v1129-pre 2002)))))

(defn test-crrbv-21
  [opts]
  ;; The following sequence of operations gave a different exception
  ;; than the above with core.rrb-vector version 0.0.14, and was a
  ;; different root cause with a distinct fix required.  I do not
  ;; recall whether it was the same root cause as
  ;; npe-for-1025-then-pop! but both test cases are included for extra
  ;; testing goodness.
  (let [v1128 (:marbles (last (play-vut opts 10 1128)))
        vpop1 (reduce (fn [v i] (pop v))
                      v1128 (range 1026))]
    (is (every? integer? (pop vpop1)))
    ;; The transient version below gave a similar exception with
    ;; version 0.0.14, but the call stack went through the transient
    ;; version of popTail, rather than the persistent version of
    ;; popTail that the one above does.
    (when (:supports-transient? opts)
      (is (every? integer? (persistent! (pop! (transient vpop1))))))))

(defn test-crrbv-22
  [{:keys [empty-vector] :as opts}]
  (when (:supports-transient? opts)
    (testing "pop! from a regular transient vector with 32*32+1 elements"
      (let [v1025 (into (empty-vector) (range 1025))]
        (is (= (persistent! (pop! (transient v1025)))
               (range 1024))))))
  (testing "pop from a persistent regular vector with 32*32+1 elements"
    (let [v1025 (into (empty-vector) (range 1025))]
      (is (= (pop v1025)
             (range 1024))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; This code was copied from
;; https://github.com/mattiasw2/adventofcode1/blob/master/src/adventofcode1/nineteen_b.clj

;; mentioned in issue
;; https://clojure.atlassian.net/projects/CRRBV/issues/CRRBV-14

(defn- puzzle-b [n seq->vec test-catvec test-subvec]
  (letfn [(remove-at [arr idx]
            (test-catvec (test-subvec arr 0 idx) (test-subvec arr (inc idx))))
          (create-arr [size]
            (seq->vec (range 1 (inc size))))
          (fv-rest [arr]
            (test-subvec arr 1))
          (calculate-opposite [n]
            (int (/ n 2)))
          (move [elfs]
            (let [lc (count elfs)]
              (if (= 1 lc)
                {:ok (first elfs)}
                (let [current      (first elfs)
                      opposite-pos (calculate-opposite lc)
                      _ (assert (> opposite-pos 0))
                      _ (assert (< opposite-pos lc))
                      opposite-elf (nth elfs opposite-pos)
                      other2       (fv-rest (remove-at elfs opposite-pos))]
                  (test-catvec other2 (seq->vec [current]))))))
          (puzzle-b-sample [elfs round]
            (let [elfs2 (move elfs)]
              ;;(println "round=" round "# elfs=" (count elfs))
              (if (:ok elfs2)
                (:ok elfs2)
                (recur elfs2 (inc round)))))]
    (puzzle-b-sample (create-arr n) 1)))

(defn- puzzle-b-core [n]
  (puzzle-b n clojure.core/vec clojure.core/into clojure.core/subvec))

;;(def custom-catvec-data (atom []))

#_(defn make-custom-catvec
  [{:keys [test-catvec]}]
  (fn [& args]
    (apply test-catvec args)))

(defn- make-custom-catvec
  [{:keys [test-catvec height max-supported-height lg-fullness]}]
  (let [num-calls (atom 0)]
    (fn [& args]
      (swap! num-calls inc)
      (let [;;n (count @custom-catvec-data)
            max-arg-height (apply max (map height args))
            ret (apply test-catvec args)
            ret-height (height ret)]
        (when (or (>= ret-height (max-supported-height))
                  (> ret-height (inc max-arg-height))
                  (< (lg-fullness ret) 0)
                  (== 1 (mod @num-calls 500)))
          (doall (map-indexed
                  (fn [idx v]
                    (println (str "custom-catvec ENTER v" idx "  "
                                  (utils/vstats v height lg-fullness))))
                  args))
          (println (str "custom-catvec LEAVE ret "
                        (utils/vstats ret height lg-fullness))))
        ;;(swap! custom-catvec-data conj {:args args :ret ret})
        ;;(println "custom-catvec RECRD in index" n "of @custom-catvec-data")
        ret))))

(defn- puzzle-b-vut
  [{:keys [seq->vec test-subvec] :as opts}
   n]
  (puzzle-b n seq->vec (make-custom-catvec opts) test-subvec))

(defn test-crrbv-14
  [opts]
  ;; This one passes with core.rrb-vector
;;  (u/reset-optimizer-counts!)
  (when do-paguro-312-failing-tests
    (is (= (puzzle-b-core 977)
           (puzzle-b-vut opts 977))))
;;  (u/print-optimizer-counts)
  ;; (puzzle-b-vut opts 978) throws
  ;; ArrayIndexOutOfBoundsException
;;  (u/reset-optimizer-counts!)
  (is (integer? (puzzle-b-vut opts 978)))
;;  (u/print-optimizer-counts)
  )


;; :seq->vec should return a vector of the desired type to be tested
;; when given any kind of seq-able collection, containing the elements
;; of that sequence in the sequence order.  it should return an empty
;; vector of the type to be tested when given an empty sequence.
    
;; :test-subvec should either be a version of subvec like
;; clojure.core/subvec for Clojure's built-in vectors, or
;; core.rrb-vector's custom version of subvec, or a custom version for
;; the type you want to test.  It should perform extra sanity checks
;; on its return value if you wish, which can of course slow down some
;; of the tests.

;; Similarly for :test-catvec and :test-pop!

;; :check-subvec and :check-catvec should be like the ones in
;; core.rrb-vector's debug namespace, except they should take only one
;; or more integers as arguments, no boolean to specify extra checks
;; or not.  That argument should be chosen by the caller using partia
;; or similar mechanism.

(defn make-empty-vector-fn [opts]
  (let [seq->vec (:seq->vec opts)]
    (fn []
      (seq->vec []))))

(defn make-check-subvec-fn [opts]
  (let [{:keys [seq->vec test-subvec same-coll?]} opts]
    (partial utils/check-subvec seq->vec test-subvec same-coll?)))

(defn make-check-catvec-fn [opts]
  (let [{:keys [seq->vec test-catvec same-coll?]} opts]
    (partial utils/check-catvec seq->vec test-catvec same-coll?)))

(defn test-all-common
  [opts]
  (assert (every? #(contains? opts %) [:seq->vec
                                       :test-subvec
                                       :test-catvec
                                       :same-coll?
                                       :height
                                       :max-supported-height
                                       :lg-fullness]))
  (let [{:keys [seq->vec test-subvec test-catvec same-coll?]} opts
        empty-vector (make-empty-vector-fn opts)
        v (empty-vector)
        opts (assoc opts
                    :empty-vector empty-vector
                    :supports-transient? (instance?
                                          clojure.lang.IEditableCollection v)
                    :check-subvec (make-check-subvec-fn opts)
                    :check-catvec (make-check-catvec-fn opts)
                    :array-kind (get opts :array-kind :object-array))]

    (when (:supports-transient? opts)
      (assert (every? #(contains? opts %) [:test-pop!])))
    (assert (contains? {:object-array :long-array} (:array-kind opts)))
    (when (= :long-array (:array-kind opts))
      (assert (every? #(contains? opts %) [:empty-vector-of])))

    (test-slicing opts)
    (test-splicing opts)
    (test-reduce opts)
    (test-reduce-2 opts)
    (test-seq opts)
    (test-assoc opts)
    (when (:supports-transient? opts)
      (test-assoc! opts))
    (test-relaxed opts)
    (test-hasheq opts)
    (test-reduce-subvec-catvec opts)
    (test-reduce-subvec-catvec2 opts)
    (test-splice-high-subtree-branch-count opts)
    (test-crrbv-12 opts)
    (when (:supports-transient? opts)
      (test-npe-for-1025-then-pop! opts))
    (test-crrbv-20 opts)
    (test-crrbv-21 opts)
    (test-crrbv-22 opts)
    (test-crrbv-14 opts)
    ))
