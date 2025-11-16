# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0] - 2025-11-15

### 🚀 Major Performance Improvements

This release focuses on **dramatic performance improvements**, making CassaFlow competitive with or faster than established libraries like Alia.

### Added
- **Automatic PreparedStatement caching**: All queries now use cached PreparedStatements for optimal performance
- **Query parsing cache**: CQL query parsing (regex + placeholder replacement) is cached to eliminate redundant processing
- **Optimized result conversion**: 
  - Use transient maps for in-place mutation during row conversion (3-8x faster)
  - Direct `.asInternal` access to CqlIdentifier avoiding string conversion
  - Efficient loop-based conversion replacing reduce
- **Comprehensive benchmark suite**: 
  - `benchmark_insert_select.clj` comparing CassaFlow vs Alia performance
  - Tests covering 100 to 100,000 rows
  - Separate INSERT and SELECT benchmarks
  - Uses fresh tables per test to avoid tombstone overhead

### Changed
- **Simplified API**: Removed `:raw?` and `:one?` options - use `first` for single results
- **Execute function** now always uses PreparedStatements (cached automatically)
- **Breaking**: `execute` now has only 2 arities: `(execute session query)` and `(execute session query params)`
- Query parameter values use `mapv` for eager evaluation and efficient array conversion
- Use `to-array` instead of `into-array Object` for better performance

### Performance Benchmarks

**vs Alia 4.3.5 (Cassandra 4.1, DataStax Driver 4.17.0):**

**INSERT Operations** (with named parameters):
- 100 rows: 12.6% faster
- 500 rows: 8.2% faster  
- 1,000 rows: **36.5% faster** 🏆
- 5,000 rows: 15.7% slower (overhead of named param mapping)
- 10,000 rows: 1.5% faster
- 50,000 rows: 9.2% faster
- 100,000 rows: 8.2% faster
- **Win rate: 85.7%** (6 out of 7 tests)

**SELECT Operations**:
- 100 rows: 12.0% faster
- 500 rows: 3.2% slower
- 1,000 rows: 6.7% faster
- 5,000 rows: 4.3% slower
- 10,000 rows: 2.0% faster
- 50,000 rows: 1.4% faster
- 100,000 rows: 3.9% slower
- **Win rate: 57.1%** (4 out of 7 tests)

**Improvement over v0.1.x:**
- SELECT operations: **3-8x faster**
- INSERT operations: **2-4x faster**

### Fixed
- Unit tests updated to work with PreparedStatement caching
- Integration tests simplified to remove deprecated options
- Type conversion for int parameters (use `Integer/valueOf` for proper type hints)

## [0.1.1] - 2024-11-14

### Changed
- Artifact ID changed to `org.clojars.caioclavico/cassaflow` for proper Clojars namespace

## [0.1.0] - 2024-11-14

### Added
- Named parameter support for CQL queries (`:param-name` syntax)
- Automatic conversion of Cassandra results to Clojure maps
- Query execution options (`:raw?`, `:one?`)
- Integration tests with Testcontainers
- Connection management via `cassaflow.client/connect`
- Support for INSERT, UPDATE, DELETE, and SELECT operations
- Core `execute` function for running CQL queries
- Query namespace with helper functions:
  - `extract-params` - Extract named parameters from CQL strings
  - `replace-placeholders` - Convert named to positional parameters
  - `prepare` - Prepare queries with parameter ordering

### Changed
- Results now return sequences of maps by default
- Map keys are keywords derived from column names

[0.2.0]: https://github.com/caioclavico/cassaflow/compare/0.1.1...0.2.0
[0.1.1]: https://github.com/caioclavico/cassaflow/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/caioclavico/cassaflow/releases/tag/0.1.0
  - `close-session` - Properly close and clean up sessions
- Full DataStax driver configuration support:
  - Basic options (contact points, datacenter, keyspace)
  - Authentication (username/password)
  - SSL/TLS support
  - External configuration file support (`application.conf`)
  - Programmatic configuration builder
  - Advanced options (timeouts, pooling, compression, etc.)
- Comprehensive test suite:
  - Unit tests for query parameter handling
  - Unit tests for core execute functionality
  - Integration tests with real Cassandra instance
- Docker support:
  - `docker-compose.yml` for local Cassandra instance
  - Integration test script with automatic container management
  - Makefile with common commands
- Documentation:
  - Complete README with examples
  - Example configuration file (`application.conf.example`)
  - Integration test examples
  - Configuration reference

### Changed
- N/A (initial release)

### Deprecated
- N/A (initial release)

### Removed
- N/A (initial release)

### Fixed
- N/A (initial release)

### Security
- N/A (initial release)

## [0.1.0-SNAPSHOT] - 2025-11-14

### Added
- Initial project structure
- Basic Leiningen configuration
- DataStax Java Driver dependency (4.15.0)
- Clojure 1.11.1 support

[Unreleased]: https://github.com/caioclavico/cassaflow/compare/v0.1.0-SNAPSHOT...HEAD
[0.1.0-SNAPSHOT]: https://github.com/caioclavico/cassaflow/releases/tag/v0.1.0-SNAPSHOT
