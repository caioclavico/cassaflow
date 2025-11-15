(defproject cassaflow "0.1.0-SNAPSHOT"
  :description "Cassandra query library for Clojure"
  :url "https://github.com/caioclavico/cassaflow"
  :license {:name "MIT License"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.datastax.oss/java-driver-core "4.17.0"]
                 [org.testcontainers/testcontainers "1.19.3"]
                 [org.testcontainers/cassandra "1.19.3"]]
  :source-paths ["src"]
  :test-paths ["test"])
