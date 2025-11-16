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

    ;; Query with parameters - returns sequence of maps
    (let [results (core/execute *session*
                                "SELECT * FROM test_ks.users WHERE id = :id"
                                {:id "user-1"})
          user (first results)]
      (is (= 1 (count results)) "Should return exactly one user")
      (is (= "user-1" (:id user)))
      (is (= "Alice" (:name user)))
      (is (= 30 (:age user))))

    ;; Query single user - returns sequence, get first
    (let [user (first (core/execute *session*
                                    "SELECT * FROM test_ks.users WHERE id = :id"
                                    {:id "user-2"}))]
      (is (map? user) "Should return a single map")
      (is (= "user-2" (:id user)))
      (is (= "Bob" (:name user)))
      (is (= 25 (:age user))))))

(deftest ^:integration test-select-all-without-params
  (testing "Select all data without parameters"
    ;; Insert test data
    (core/execute *session*
                  "INSERT INTO test_ks.users (id, name, age) VALUES (:id, :name, :age)"
                  {:id "user-1" :name "Alice" :age (int 30)})

    (core/execute *session*
                  "INSERT INTO test_ks.users (id, name, age) VALUES (:id, :name, :age)"
                  {:id "user-2" :name "Bob" :age (int 25)})

    (core/execute *session*
                  "INSERT INTO test_ks.users (id, name, age) VALUES (:id, :name, :age)"
                  {:id "user-3" :name "Charlie" :age (int 35)})

    ;; Query all users - returns sequence of maps
    (let [users (core/execute *session* "SELECT * FROM test_ks.users")
          users-vec (vec users)]
      (is (= 3 (count users-vec)) "Should return 3 users")
      (is (every? map? users-vec) "All results should be maps")
      (is (every? #(contains? % :id) users-vec) "All maps should have :id")
      (is (every? #(contains? % :name) users-vec) "All maps should have :name")
      (is (every? #(contains? % :age) users-vec) "All maps should have :age"))))

(deftest ^:integration test-update-operation
  (testing "Update existing data"
    ;; Insert initial data
    (core/execute *session*
                  "INSERT INTO test_ks.users (id, name, age) VALUES (:id, :name, :age)"
                  {:id "user-1" :name "Alice" :age (int 30)})

    ;; Update the user
    (core/execute *session*
                  "UPDATE test_ks.users SET age = :age WHERE id = :id"
                  {:id "user-1" :age (int 31)})

    ;; Verify update
    (let [user (first (core/execute *session*
                                    "SELECT * FROM test_ks.users WHERE id = :id"
                                    {:id "user-1"}))]
      (is (= "user-1" (:id user)))
      (is (= "Alice" (:name user)))
      (is (= 31 (:age user)) "Age should be updated to 31"))))

(deftest ^:integration test-delete-operation
  (testing "Delete existing data"
    ;; Insert test data
    (core/execute *session*
                  "INSERT INTO test_ks.users (id, name, age) VALUES (:id, :name, :age)"
                  {:id "user-1" :name "Alice" :age (int 30)})

    (core/execute *session*
                  "INSERT INTO test_ks.users (id, name, age) VALUES (:id, :name, :age)"
                  {:id "user-2" :name "Bob" :age (int 25)})

    ;; Verify both users exist
    (let [users-before (core/execute *session* "SELECT * FROM test_ks.users")]
      (is (= 2 (count users-before)) "Should have 2 users before delete"))

    ;; Delete one user
    (core/execute *session*
                  "DELETE FROM test_ks.users WHERE id = :id"
                  {:id "user-1"})

    ;; Verify deletion
    (let [users-after (core/execute *session* "SELECT * FROM test_ks.users")
          remaining-user (first users-after)]
      (is (= 1 (count users-after)) "Should have 1 user after delete")
      (is (= "user-2" (:id remaining-user)) "Remaining user should be user-2"))))

(deftest ^:integration test-batch-insert
  (testing "Batch insert multiple records"
    ;; Insert multiple users
    (doseq [i (range 1 6)]
      (core/execute *session*
                    "INSERT INTO test_ks.users (id, name, age) VALUES (:id, :name, :age)"
                    {:id (str "user-" i)
                     :name (str "User" i)
                     :age (int (+ 20 i))}))

    ;; Verify all were inserted
    (let [users (core/execute *session* "SELECT * FROM test_ks.users")
          user-count (count users)]
      (is (= 5 user-count) "Should have 5 users")

      ;; Verify we can query individual users as maps
      (let [user-3 (first (core/execute *session*
                                        "SELECT * FROM test_ks.users WHERE id = :id"
                                        {:id "user-3"}))]
        (is (= "user-3" (:id user-3)))
        (is (= "User3" (:name user-3)))
        (is (= 23 (:age user-3)))))))