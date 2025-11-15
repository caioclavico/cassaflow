(ns cassaflow.integration-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [cassaflow.core :as core]
   [cassaflow.client :as client])
  (:import
   [org.testcontainers.containers CassandraContainer]))

(def ^:dynamic *cassandra-container* nil)
(def ^:dynamic *session* nil)

(defn start-cassandra-container []
  (doto (CassandraContainer. "cassandra:4.1")
    (.start)))

(defn stop-cassandra-container [container]
  (.stop container))

(defn create-session [container]
  (let [host (.getHost container)
        port (.getFirstMappedPort container)]
    (client/connect {:host host
                     :port port})))

(defn setup-test-schema [session]
  ;; Create keyspace
  (core/execute session 
                "CREATE KEYSPACE IF NOT EXISTS test_ks WITH replication = {'class':'SimpleStrategy', 'replication_factor':1}")
  
  ;; Create table
  (core/execute session 
                "CREATE TABLE IF NOT EXISTS test_ks.users (id text PRIMARY KEY, name text, age int)")
  
  ;; Create another table for more complex tests
  (core/execute session
                "CREATE TABLE IF NOT EXISTS test_ks.products (id uuid PRIMARY KEY, name text, price decimal, quantity int)"))

(defn cleanup-test-data [session]
  ;; Truncate tables to remove all data but keep schema
  (core/execute session "TRUNCATE test_ks.users")
  (core/execute session "TRUNCATE test_ks.products"))

(defn cleanup-fixture [f]
  (f)
  (cleanup-test-data *session*))

(defn integration-fixture [f]
  (let [container (start-cassandra-container)
        session (create-session container)]
    (try
      (setup-test-schema session)
      (binding [*cassandra-container* container
                *session* session]
        (f))
      (finally
        ;; Drop keyspace to clean up everything
        (try
          (core/execute session "DROP KEYSPACE IF EXISTS test_ks")
          (catch Exception e
            (println "Error dropping keyspace:" (.getMessage e))))
        (.close session)
        (stop-cassandra-container container)))))

(use-fixtures :once integration-fixture)
(use-fixtures :each cleanup-fixture)

(deftest ^:integration test-insert-and-select-with-params
  (testing "Insert and select data using named parameters"
    ;; Insert data
    (core/execute *session* 
                  "INSERT INTO test_ks.users (id, name, age) VALUES (:id, :name, :age)"
                  {:id "user-1" :name "Alice" :age (int 30)})
    
    (core/execute *session* 
                  "INSERT INTO test_ks.users (id, name, age) VALUES (:id, :name, :age)"
                  {:id "user-2" :name "Bob" :age (int 25)})
    
    ;; Query with parameters
    (let [result (core/execute *session* 
                               "SELECT * FROM test_ks.users WHERE id = :id"
                               {:id "user-1"})
          row (first (iterator-seq (.iterator result)))]
      
      (is (some? row) "Should retrieve a row")
      (is (= "user-1" (.getString row "id")))
      (is (= "Alice" (.getString row "name")))
      (is (= 30 (.getInt row "age"))))))

(deftest ^:integration test-select-all-without-params
  (testing "Select all data without parameters"
    ;; Insert test data
    (core/execute *session* 
                  "INSERT INTO test_ks.users (id, name, age) VALUES (:id, :name, :age)"
                  {:id "user-3" :name "Charlie" :age (int 35)})
    
    (core/execute *session* 
                  "INSERT INTO test_ks.users (id, name, age) VALUES (:id, :name, :age)"
                  {:id "user-4" :name "Diana" :age (int 28)})
    
    ;; Query without parameters
    (let [result (core/execute *session* "SELECT * FROM test_ks.users")
          rows (vec (iterator-seq (.iterator result)))]
      
      (is (>= (count rows) 2) "Should have at least 2 users"))))

(deftest ^:integration test-update-operation
  (testing "Update existing record"
    ;; Insert initial data
    (core/execute *session* 
                  "INSERT INTO test_ks.users (id, name, age) VALUES (:id, :name, :age)"
                  {:id "user-5" :name "Eve" :age (int 22)})
    
    ;; Update the record
    (core/execute *session* 
                  "UPDATE test_ks.users SET age = :age WHERE id = :id"
                  {:id "user-5" :age (int 23)})
    
    ;; Verify update
    (let [result (core/execute *session* 
                               "SELECT * FROM test_ks.users WHERE id = :id"
                               {:id "user-5"})
          row (first (iterator-seq (.iterator result)))]
      
      (is (= 23 (.getInt row "age")) "Age should be updated to 23"))))

(deftest ^:integration test-delete-operation
  (testing "Delete a record"
    ;; Insert data
    (core/execute *session* 
                  "INSERT INTO test_ks.users (id, name, age) VALUES (:id, :name, :age)"
                  {:id "user-6" :name "Frank" :age (int 40)})
    
    ;; Verify it exists
    (let [result-before (core/execute *session* 
                                      "SELECT * FROM test_ks.users WHERE id = :id"
                                      {:id "user-6"})
          row-before (first (iterator-seq (.iterator result-before)))]
      (is (some? row-before) "User should exist before delete"))
    
    ;; Delete the record
    (core/execute *session* 
                  "DELETE FROM test_ks.users WHERE id = :id"
                  {:id "user-6"})
    
    ;; Verify deletion
    (let [result-after (core/execute *session* 
                                     "SELECT * FROM test_ks.users WHERE id = :id"
                                     {:id "user-6"})
          row-after (first (iterator-seq (.iterator result-after)))]
      (is (nil? row-after) "User should not exist after delete"))))

(deftest ^:integration test-batch-insert
  (testing "Insert multiple records and verify count"
    (doseq [i (range 10)]
      (core/execute *session* 
                    "INSERT INTO test_ks.users (id, name, age) VALUES (:id, :name, :age)"
                    {:id (str "batch-user-" i) 
                     :name (str "User " i) 
                     :age (int (+ 20 i))}))
    
    (let [result (core/execute *session* "SELECT COUNT(*) FROM test_ks.users")
          count-row (first (iterator-seq (.iterator result)))
          total-count (.getLong count-row 0)]
      
      (is (>= total-count 10) "Should have at least 10 users from batch insert"))))