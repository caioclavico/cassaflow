(ns cassaflow.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [cassaflow.core :as cf])
  (:import
   [com.datastax.oss.driver.api.core CqlSession]
   [com.datastax.oss.driver.api.core.cql ResultSet]))

(deftest test-execute-builds-statement
  (testing "execute should build correct CQL statement with named parameters"
    (let [executed-stmt (atom nil)
          fake-result-set (reify ResultSet
                            (iterator [_] (.iterator [])))

          fake-session
          (proxy [CqlSession] []
            (execute [stmt]
              (reset! executed-stmt stmt)
              fake-result-set))]

      (cf/execute fake-session
                  "SELECT * FROM users WHERE id = :id"
                  {:id "123"})

      ;; Check if statement was executed
      (is (some? @executed-stmt) "Statement should be executed")

      ;; Validate that we received a statement (might be String or SimpleStatement)
      (let [query (if (string? @executed-stmt)
                    @executed-stmt
                    (.getQuery @executed-stmt))]
        (is (= "SELECT * FROM users WHERE id = ?"
               query)
            "CQL should replace named parameters with positional ones")))))

(deftest test-execute-without-params
  (testing "execute should work without parameters"
    (let [executed-stmt (atom nil)
          fake-result-set (reify ResultSet
                            (iterator [_] (.iterator [])))

          fake-session
          (proxy [CqlSession] []
            (execute [stmt]
              (reset! executed-stmt stmt)
              fake-result-set))]

      (cf/execute fake-session "SELECT * FROM users")

      (is (some? @executed-stmt) "Statement should be executed")

      (let [query (if (string? @executed-stmt)
                    @executed-stmt
                    (.getQuery @executed-stmt))]
        (is (= "SELECT * FROM users" query))))))

(deftest test-execute-with-multiple-params
  (testing "execute should handle multiple named parameters"
    (let [executed-stmt (atom nil)
          fake-result-set (reify ResultSet
                            (iterator [_] (.iterator [])))

          fake-session
          (proxy [CqlSession] []
            (execute [stmt]
              (reset! executed-stmt stmt)
              fake-result-set))]

      (cf/execute fake-session
                  "SELECT * FROM users WHERE id = :id AND name = :name"
                  {:id "123" :name "John"})

      (let [query (if (string? @executed-stmt)
                    @executed-stmt
                    (.getQuery @executed-stmt))]
        (is (= "SELECT * FROM users WHERE id = ? AND name = ?"
               query))))))