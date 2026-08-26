# Job Application Orchestrator

Automation of job discovery and application. The orchestrator ingests candidate documents, discovers jobs from multiple sources, scores them against configurable filters, and exposes the results through a local dashboard and an agent-friendly CLI.

## Requirements

* JDK 21
* Maven 3.9+

## Build

```bash
mvn verify
```

This compiles the project and runs the test suite.

## Code Style

Formatting is enforced with [Spotless](https://github.com/diffplug/spotless) using [Palantir Java Format](https://github.com/palantir/palantir-java-format) (4-space indent, 120-column limit, one argument per line when wrapping), plus import ordering, unused import removal, trailing whitespace trimming, and a newline at end of file.

```bash
mvn spotless:apply
mvn spotless:check
```

The plugin is not bound to any lifecycle phase, so regular builds (`mvn package`, `mvn install`) never trigger it.

## Continuous Integration

A GitHub Actions workflow runs on every pull request:

1. `mvn spotless:check` — fails if the code is not properly formatted
2. `mvn verify` — builds the project and runs the tests

## Roadmap

### MVP — `0.0.1`

* Core: domain model, filter configuration, PDF ingestor, matching engine
* Job discovery: job source abstraction with one real source
* Persistence and interface: local persistence, minimal dashboard

### After MVP Consolidation

* Discovery: search engines, multiple ATS platforms, advanced deduplication
* Application automation via browser automation (approach TBD)
* Scalability: distributed workers
* Agent-friendly CLI for ingestion analysis, job analysis, matching assistance, and natural-language filtering
