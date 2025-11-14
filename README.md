# CassaFlow

A lightweight Clojure library for Apache Cassandra that provides a simple and idiomatic interface for executing CQL queries with named parameters.

## Features

- **Named Parameters**: Use `:param-name` syntax in your CQL queries instead of positional `?` placeholders
- **Automatic Parameter Binding**: Parameters are automatically extracted and bound in the correct order
- **Simple API**: Minimal boilerplate with a clean, functional interface
- **DataStax Driver Integration**: Built on top of the official DataStax Java Driver for Cassandra

## Installation

Add the following dependency to your `project.clj`:

```clojure
[cassaflow "0.1.0-SNAPSHOT"]
```

## Usage

### Connecting to Cassandra

First, create a CqlSession using the DataStax driver:

```clojure
(require '[cassaflow.client :as client])

;; Create a session
(def session (client/create-session {:contact-points ["127.0.0.1"]
                                      :local-datacenter "datacenter1"
                                      :keyspace "my_keyspace"}))
```

### Executing Queries

Use named parameters in your CQL queries:

```clojure
(require '[cassaflow.core :as cf])

;; Simple SELECT with named parameters
(cf/execute session
            "SELECT * FROM users WHERE id = :id AND active = :active"
            {:id "user-123" :active true})

;; INSERT with named parameters
(cf/execute session
            "INSERT INTO users (id, name, email, age) VALUES (:id, :name, :email, :age)"
            {:id "user-456"
             :name "John Doe"
             :email "john@example.com"
             :age 30})

;; UPDATE with named parameters
(cf/execute session
            "UPDATE users SET name = :name, email = :email WHERE id = :id"
            {:id "user-123"
             :name "Jane Doe"
             :email "jane@example.com"})

;; DELETE with named parameters
(cf/execute session
            "DELETE FROM users WHERE id = :id"
            {:id "user-123"})

;; Query without parameters
(cf/execute session "SELECT * FROM users")
```

### Working with Query Results

The `execute` function returns a `ResultSet` that you can process:

```clojure
(let [result (cf/execute session
                         "SELECT * FROM users WHERE active = :active"
                         {:active true})]
  ;; Process results
  (doseq [row result]
    (println "User:" (.getString row "name"))))
```

### Building Queries Programmatically

You can also use the query namespace to prepare queries:

```clojure
(require '[cassaflow.query :as q])

;; Prepare a query with named parameters
(let [{:keys [cql params]} (q/prepare
                             "SELECT * FROM users WHERE id = :id AND age > :age"
                             {:id "user-123" :age 18})]
  (println "CQL:" cql)        ;; "SELECT * FROM users WHERE id = ? AND age > ?"
  (println "Params:" params)) ;; ["user-123" 18]
```

## API Reference

### `cassaflow.core`

- `(execute session cql-string params-map)` - Execute a CQL query with named parameters

### `cassaflow.query`

- `(extract-params cql-string)` - Extract named parameter keywords from a CQL string
- `(replace-placeholders cql-string)` - Replace named parameters with `?` placeholders
- `(prepare cql-string params-map)` - Prepare a query by converting named to positional parameters

### `cassaflow.client`

- `(create-session config-map)` - Create a new CqlSession with the given configuration

## Configuration Options

When creating a session with `create-session`, you can provide:

```clojure
{:contact-points ["127.0.0.1" "127.0.0.2"]  ;; Cassandra node addresses
 :local-datacenter "datacenter1"             ;; Local datacenter name
 :keyspace "my_keyspace"                     ;; Default keyspace (optional)
 :port 9042}                                 ;; Port number (optional, default: 9042)
```

## Examples

### User Management

```clojure
;; Create a user
(cf/execute session
            "INSERT INTO users (id, name, email, created_at) 
             VALUES (:id, :name, :email, :created_at)"
            {:id (java.util.UUID/randomUUID)
             :name "Alice Smith"
             :email "alice@example.com"
             :created_at (java.time.Instant/now)})

;; Find active users
(cf/execute session
            "SELECT * FROM users WHERE active = :active ALLOW FILTERING"
            {:active true})

;; Update user email
(cf/execute session
            "UPDATE users SET email = :email WHERE id = :id"
            {:id user-id
             :email "newemail@example.com"})
```

### Batch Operations

```clojure
;; Note: For batch operations, use the DataStax driver's BatchStatement
;; CassaFlow focuses on simple query execution
```

## Testing

Run the test suite:

```bash
lein test
```

Run specific tests:

```bash
lein test :only cassaflow.core-test
lein test :only cassaflow.query-test
```

## Requirements

- Clojure 1.11.0 or higher
- DataStax Java Driver for Apache Cassandra 4.x

## License

Copyright © 2025

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Roadmap
- [ ] Full driver configuration access
- [ ] Async query execution support
- [ ] Prepared statement caching
- [ ] Query builder DSL
- [ ] Better error handling and validation
- [ ] Performance benchmarks
- [ ] More comprehensive documentation

## Support

For issues, questions, or contributions, please visit the [GitHub repository](https://github.com/caioclavico/cassaflow).
