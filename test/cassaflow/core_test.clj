(ns cassaflow.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [cassaflow.core :as cf])
  (:import
   [com.datastax.oss.driver.api.core CqlSession]
   [com.datastax.oss.driver.api.core.cql ResultSet PreparedStatement BoundStatement]))

(deftest test-execute-builds-statement
  (testing "execute should build correct CQL statement with named parameters"
    (let [executed-bound-stmt (atom nil)
          prepared-cql (atom nil)
          fake-result-set (reify ResultSet
                            (iterator [_] (.iterator [])))

          fake-prepared-stmt
          (reify PreparedStatement
            (bind [_ values]
              (reset! executed-bound-stmt values)
              (reify BoundStatement)))

          fake-session
          (proxy [CqlSession] []
            (execute [stmt]
              fake-result-set)
            (prepare [cql]
              (reset! prepared-cql cql)
              fake-prepared-stmt))]

      (cf/execute fake-session
                  "SELECT * FROM users WHERE id = :id"
                  {:id "123"})

      ;; Check if PreparedStatement was created with correct CQL
      (is (= "SELECT * FROM users WHERE id = ?"
             @prepared-cql)
          "CQL should replace named parameters with positional ones")

      ;; Check if values were bound
      (is (some? @executed-bound-stmt) "Values should be bound to statement"))))

(deftest test-execute-without-params
  (testing "execute should work without parameters"
    (let [prepared-cql (atom nil)
          fake-result-set (reify ResultSet
                            (iterator [_] (.iterator [])))

          fake-prepared-stmt
          (reify PreparedStatement
            (bind [_ values]
              (reify BoundStatement)))

          fake-session
          (proxy [CqlSession] []
            (execute [stmt]
              fake-result-set)
            (prepare [cql]
              (reset! prepared-cql cql)
              fake-prepared-stmt))]

      (cf/execute fake-session "SELECT * FROM users")

      (is (= "SELECT * FROM users" @prepared-cql)))))

(deftest test-execute-with-multiple-params
  (testing "execute should handle multiple named parameters"
    (let [prepared-cql (atom nil)
          bound-values (atom nil)
          fake-result-set (reify ResultSet
                            (iterator [_] (.iterator [])))

          fake-prepared-stmt
          (reify PreparedStatement
            (bind [_ values]
              (reset! bound-values values)
              (reify BoundStatement)))

          fake-session
          (proxy [CqlSession] []
            (execute [stmt]
              fake-result-set)
            (prepare [cql]
              (reset! prepared-cql cql)
              fake-prepared-stmt))]

      (cf/execute fake-session
                  "SELECT * FROM users WHERE id = :id AND name = :name"
                  {:id "123" :name "John"})

      (is (= "SELECT * FROM users WHERE id = ? AND name = ?"
             @prepared-cql)))))