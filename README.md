# cassaflow

A Clojure library for working with Apache Cassandra, providing idiomatic Clojure interfaces with named parameter support and automatic result conversion.

## Features

- 🔌 Simple connection management
- 📝 Named parameter support (`:param` style)
- 🗺️ Automatic conversion of results to Clojure maps
- 🎯 Flexible query execution with options
- 🧪 Full integration test suite with Testcontainers
- ⚡ Built on DataStax Java Driver 4.x

## Installation

Add the following dependency to your `project.clj`:

```clojure
[org.clojars.caioclavico/cassaflow "0.1.1"]
```

Or in `deps.edn`:

```clojure
{:deps {org.clojars.caioclavico/cassaflow {:mvn/version "0.1.1"}}}
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

;; Use named parameters
(cass/execute session 
              "SELECT * FROM users WHERE id = :id" 
              {:id "1"})
;; => ({:id "1" :name "Alice" :age 30})

;; Get a single result
(cass/execute session 
              "SELECT * FROM users WHERE id = :id" 
              {:id "1"}
              {:one? true})
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

;; Get single result with :one? option
(cass/execute session 
              "SELECT * FROM users WHERE id = :id" 
              {:id "1"}
              {:one? true})
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

;; Get raw ResultSet if needed
(cass/execute session 
              "SELECT * FROM users"
              nil
              {:raw? true})
;; => #<ResultSet ...>
```

### Working with Results

Results are automatically converted to Clojure maps with keyword keys:

```clojure
(let [users (cass/execute session "SELECT * FROM users")]
  (doseq [user users]
    (println (:name user) "is" (:age user) "years old")))
```

## Query Options

The `execute` function accepts an optional map of options:

- `:raw?` - Returns the raw `ResultSet` instead of converted maps (default: `false`)
- `:one?` - Returns a single map instead of a sequence (default: `false`)

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

