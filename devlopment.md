# Job Application Orchestrator

## Purpose

Automation of job discovery and application.

---

## MVP — `0.0.1`

### Core

* Domain Model
* Filter Configuration
* PDF Ingestor
* Matching Engine

### Job Discovery

* Job Source Abstraction

    * One real source

### Persistence & Interface

* Local Persistence
* Minimal Dashboard

---

## After MVP Consolidation

### Discovery

* Search Engines
* Multiple ATS
* Advanced Deduplication

### Application

* Application Automation
* Browser Automation

    * TBD

        * Browser Extension
        * Playwright
        * Alternative approach

### Scalability

* Distributed Workers

---

## Additional Features

### Agent-Friendly CLI

#### Ingestion Analysis

* Read candidate profile and historical data
* Generate and refine reliable keywords
* Identify relevant role and technology variations

#### Job Analysis

* Read job description
* Generate a concise summary
* Highlight strong matches
* Identify missing requirements
* Identify potential conflicts

#### Matching Assistance

* Explain why a job received its score
* Suggest improvements to filter configuration
* Suggest relevant keywords and synonyms

#### Job Filtering

* Filter by score, technology, seniority, location, salary, etc.
* Support natural-language queries for job discovery and filtering
