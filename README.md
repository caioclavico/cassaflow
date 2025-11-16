# cassaflow

A **high-performance** Clojure library for working with Apache Cassandra, providing idiomatic Clojure interfaces with named parameter support and automatic result conversion.

**🚀 Performance:** CassaFlow now **matches or exceeds** the performance of established libraries like Alia, winning **86% of INSERT benchmarks** and **57% of SELECT benchmarks** through aggressive optimization.

## Features

- 🔌 Simple connection management
- 📝 Named parameter support (`:param` style) with zero overhead
- ⚡ **Automatic PreparedStatement caching** for optimal performance
- 🗺️ **Optimized result conversion** using transient maps and direct CQL access
- 🎯 Clean, simple API - no extra options needed
- 🧪 Full integration test suite with Testcontainers
- 🏗️ Built on DataStax Java Driver 4.x
- 📊 **Extensively benchmarked** against production workloads

## Performance

CassaFlow v0.2.0 includes major performance optimizations:

- **3-8x faster** than v0.1.x for SELECT operations
- **2-4x faster** than v0.1.x for INSERT operations
- **Competitive with Alia**: Wins 86% of INSERT tests and 57% of SELECT tests
- PreparedStatement caching eliminates query parsing overhead
- Transient collections for zero-copy result conversion
- Query parsing cache for repeated queries

### Benchmark Results (vs Alia 4.3.5)

**INSERT Performance (with named parameters):**
- 100 rows: **-12.6%** faster 🏆
- 1,000 rows: **-36.5%** faster 🏆
- 10,000 rows: **-1.5%** faster 🏆
- 100,000 rows: **-8.2%** faster 🏆

**SELECT Performance:**
- 100 rows: **-12.0%** faster 🏆
- 1,000 rows: **-6.7%** faster 🏆
- 10,000 rows: **-2.0%** faster 🏆
- 50,000 rows: **-1.4%** faster 🏆

*Negative percentages indicate CassaFlow is faster. Benchmarks run on Cassandra 4.1 with DataStax Driver 4.17.0.*

## Installation

Add the following dependency to your `project.clj`:

```clojure
[org.clojars.caioclavico/cassaflow "0.2.0"]
```

Or in `deps.edn`:

```clojure
{:deps {org.clojars.caioclavico/cassaflow {:mvn/version "0.2.0"}}}
```

## Quick Start

```clojure
(require '[cassaflow.client :as client])
(require '[cassaflow.core :as cass])

;; Connect to Cassandra
(def session (client/connect {:host "127.0.0.1" :port 9042}))

;; Execute queries with automatic map conversion
(cass/execute session "SELECT * FROM users")
;; => ({:id "1" :name "Alice" :age 30} {:id "2" :name "Bob" :age 25})

;; Use named parameters - PreparedStatements are cached automatically!
(cass/execute session 
              "SELECT * FROM users WHERE id = :id" 
              {:id "1"})
;; => ({:id "1" :name "Alice" :age 30})

;; Insert with named parameters (uses cached PreparedStatement)
(cass/execute session
              "INSERT INTO users (id, name, age) VALUES (:id, :name, :age)"
              {:id "3" :name "Charlie" :age 35})

;; Get a single result with first
(first (cass/execute session 
                     "SELECT * FROM users WHERE id = :id" 
                     {:id "1"}))
;; => {:id "1" :name "Alice" :age 30}

;; Clean up
(.close session)
```

## Usage

### Connecting to Cassandra

```clojure
(require '[cassaflow.client :as client])

;; Connect with default settings (localhost:9042)
(def session (client/connect))

;; Connect to a specific host and port
(def session (client/connect {:host "cassandra.example.com" 
                              :port 9042}))

;; Don't forget to close the session when done
(.close session)
```

### Executing Queries

```clojure
(require '[cassaflow.core :as cass])

;; Simple SELECT - returns sequence of maps
(cass/execute session "SELECT * FROM users")
;; => ({:id "1" :name "Alice" :age 30} {:id "2" :name "Bob" :age 25})

;; SELECT with named parameters
(cass/execute session 
              "SELECT * FROM users WHERE id = :id" 
              {:id "1"})
;; => ({:id "1" :name "Alice" :age 30})

;; Get single result using first
(first (cass/execute session 
                     "SELECT * FROM users WHERE id = :id" 
                     {:id "1"}))
;; => {:id "1" :name "Alice" :age 30}

;; INSERT with named parameters
(cass/execute session
              "INSERT INTO users (id, name, age) VALUES (:id, :name, :age)"
              {:id "3" :name "Charlie" :age 35})

;; UPDATE with named parameters
(cass/execute session
              "UPDATE users SET age = :age WHERE id = :id"
              {:id "1" :age 31})

;; DELETE with named parameters
(cass/execute session
              "DELETE FROM users WHERE id = :id"
              {:id "3"})
```

### Working with Results

Results are automatically converted to Clojure maps with keyword keys:

```clojure
(let [users (cass/execute session "SELECT * FROM users")]
  (doseq [user users]
    (println (:name user) "is" (:age user) "years old")))
```

## Development

### Running Tests

```bash
# Run all tests
lein test

# Run only unit tests
lein test cassaflow.core-test cassaflow.query-test

# Run only integration tests (requires Docker)
lein test :integration
```

### Integration Tests

Integration tests use Testcontainers to spin up a real Cassandra instance in Docker. Make sure Docker is running before executing integration tests.

## License

Copyright © 2024

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the Classpath Exception which is available at
https://www.gnu.org/software/classpath/license.html.

## Contributing

Contributions are welcome! Please visit the [GitHub repository](https://github.com/caioclavico/cassaflow).

