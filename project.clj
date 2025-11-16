(defproject org.clojars.caioclavico/cassaflow "0.2.0"
  :description "A high-performance Clojure library for Apache Cassandra with named parameters, automatic PreparedStatement caching, and optimized result conversion"
  :url "https://github.com/caioclavico/cassaflow"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.datastax.oss/java-driver-core "4.17.0"]]
  :profiles {:dev {:dependencies [[org.testcontainers/testcontainers "1.19.3"]
                                  [org.testcontainers/cassandra "1.19.3"]
                                  [criterium "0.4.6"]
                                  [cc.qbits/alia "4.3.5"]
                                  [com.codahale.metrics/metrics-core "3.0.2"]]}}
  :source-paths ["src"]
  :test-paths ["test"]
  :test-selectors {:default (complement :integration)
                   :integration :integration}
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :sign-releases false}]]
  :scm {:name "git"
        :url "https://github.com/caioclavico/cassaflow"})
