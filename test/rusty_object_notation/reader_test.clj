(ns rusty-object-notation.reader-test
  (:require [clojure.test :refer [deftest is testing]]
            [rusty-object-notation.reader :as ron.reader]))

(deftest do-next
  (testing
    "simple"
    (is (= {::ron.reader/dispatch-char 1
            ::ron.reader/next-chars    [2]}
           (ron.reader/do-next {::ron.reader/next-chars [1 2]})))))

(deftest string-reader-impl
  (testing
    "simple"
    (is (= {::ron.reader/dispatch-char nil
            ::ron.reader/next-chars    nil
            ::ron.reader/value         "a"}
           (ron.reader/string-reader-impl
             {::ron.reader/next-chars [\a \"]})))))


(deftest number-reader-impl
  (testing
    "simple"
    (is (= {::ron.reader/dispatch-char \space
            ::ron.reader/next-chars    []
            ::ron.reader/value         1}
           (ron.reader/number-reader-impl
             {::ron.reader/next-chars [\1 \space]})))))


(deftest true-reader-impl
  (testing
    "simple"
    (is (= {::ron.reader/dispatch-char \space
            ::ron.reader/next-chars    nil
            ::ron.reader/value         true}
           (ron.reader/true-reader-impl
             {::ron.reader/next-chars [\r \u \e \space]})))))



(deftest false-reader-impl
  (testing
    "simple"
    (is (= {::ron.reader/dispatch-char \space
            ::ron.reader/next-chars    nil
            ::ron.reader/value         false}
           (ron.reader/false-reader-impl
             {::ron.reader/next-chars [\a \l \s \e \space]})))))

(deftest read-symbol-impl
  (testing
    "simple"
    (is (= {::ron.reader/dispatch-char \space
            ::ron.reader/next-chars    []
            ::ron.reader/value         'abc}
           (ron.reader/read-symbol-impl
             {::ron.reader/dispatch-char \a
              ::ron.reader/next-chars    [\b \c \space]})))))

(deftest comma-or-colon
  (testing
    "simple"
    (is (= {::ron.reader/dispatch-char \:
            ::ron.reader/next-chars    []}
           (ron.reader/comma-or-colon
             {::ron.reader/dispatch-char \space
              ::ron.reader/next-chars    [\:]})))))

(deftest collection-reader-impl
  (testing
    "simple"
    (is (= {::ron.reader/dispatch-char nil,
            ::ron.reader/next-chars    (),
            ::ron.reader/kv?           true,
            ::ron.reader/value         {}}
           (-> {::ron.reader/dispatch-char \{
                ::ron.reader/next-chars    [\}]}
               ron.reader/collection-reader-impl
               (dissoc ::ron.reader/coll)
               #_(doto clojure.pprint/pprint))))))


(deftest json
  (testing
    "json data"
    (is (= {}
           (-> "{}"
               ron.reader/read-string
               #_(doto clojure.pprint/pprint))))
    (is (= {"a" 1}
           (-> "{\"a\": 1}"
               ron.reader/read-string
               #_(doto clojure.pprint/pprint))))
    (is (= ["a" 1]
           (-> "[\"a\", 1]"
               ron.reader/read-string
               #_(doto clojure.pprint/pprint))))))
