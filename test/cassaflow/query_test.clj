(ns cassaflow.query-test
  (:require [clojure.test :refer [deftest is testing]]
            [cassaflow.query :as q]))

(deftest test-extract-params
  (testing "extract-params should find all named parameters in CQL"
    (is (= [:id :active]
           (q/extract-params "SELECT * FROM users WHERE id = :id AND active = :active"))
        "Should extract parameters in order of appearance")

    (is (= []
           (q/extract-params "SELECT * FROM users"))
        "Should return empty vector when no parameters")))

(deftest test-replace-placeholders
  (testing "replace-placeholders should convert named to positional parameters"
    (is (= "SELECT * FROM users WHERE id = ? AND active = ?"
           (q/replace-placeholders "SELECT * FROM users WHERE id = :id AND active = :active"))
        "Should replace :param with ?")

    (is (= "SELECT * FROM users"
           (q/replace-placeholders "SELECT * FROM users"))
        "Should handle queries without parameters")))

(deftest test-prepare
  (testing "prepare should convert CQL with named params to positional"
    (let [r (q/prepare "SELECT * FROM users WHERE id = :id AND active = :active"
                       {:id "abc" :active true})]
      (is (= "SELECT * FROM users WHERE id = ? AND active = ?"
             (:cql r))
          "CQL should have positional placeholders")
      (is (= ["abc" true]
             (:params r))
          "Parameters should be ordered according to appearance in CQL")))

  (testing "prepare should handle queries without parameters"
    (let [r (q/prepare "SELECT * FROM users" {})]
      (is (= "SELECT * FROM users" (:cql r)))
      (is (= [] (:params r)))))

  (testing "prepare should handle missing parameters"
    (let [r (q/prepare "SELECT * FROM users WHERE id = :id" {})]
      (is (= "SELECT * FROM users WHERE id = ?" (:cql r)))
      (is (= [nil] (:params r))
          "Missing parameters should be nil"))))
