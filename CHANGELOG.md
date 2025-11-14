# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Named parameter support for CQL queries (`:param-name` syntax)
- Automatic parameter extraction and binding
- Core `execute` function for running CQL queries with named parameters
- Query namespace with helper functions:
  - `extract-params` - Extract named parameters from CQL strings
  - `replace-placeholders` - Convert named to positional parameters
  - `prepare` - Prepare queries with parameter ordering
- Client namespace for session management:
  - `create-session` - Create CqlSession with comprehensive configuration
  - `create-programmatic-config` - Build driver config programmatically
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
