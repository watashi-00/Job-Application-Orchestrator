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


#### Possible Layout

```
┌─────────────────────────────────────────────────────────────────────┐
│                          <APPLICATION NAME>                         │
├──────────────────┬─────────────────────────┬────────────────────────┤
│ NAVBAR           │ CONTENT                 │ AGENT                  │
├──────────────────┼─────────────────────────┼────────────────────────┤
│                  │                         │                        │
│                  │                         │                        │
│                  │                         │                        │
│                  │                         │                        │
│                  │                         │                        │
│                  │                         │                        │
│                  │                         │                        │
└──────────────────┴─────────────────────────┴────────────────────────┘
```

#### Possible future features
* Email Inbox Integration
    * Detect recruiter/contact emails
    * Detect application status changes
    * Associate conversations with applications
    * Extract interview dates and deadlines

* Calendar Integration
    * Detect scheduled interviews
    * Create/update interview events
    * Track technical interviews and meetings

* Application Tracking
    * Track application lifecycle
    * Detect stale applications
    * Track response time
    * Maintain application history

* Document Management
    * Multiple CV versions
    * Cover letters
    * Portfolio versions
    * Job-specific documents

* Interview Management
    * Interview preparation notes
    * Job-specific technical topics
    * Questions asked in previous interviews
    * Personal feedback and notes

* Recruiter & Company Intelligence
    * Company profile
    * Recruiter information
    * Previous interactions
    * Company/application history

* Job History & Analytics
    * Application conversion rate
    * Response rate
    * Interview rate
    * Offer rate
    * Rejection rate
    * Average time between stages
    * Performance by job source

* Application Recommendations
    * Recommend whether to apply
    * Identify missing requirements
    * Estimate application priority
    * Suggest CV/profile variant

* Follow-up Automation
    * Track when follow-up is appropriate
    * Generate follow-up drafts
    * Remind about pending responses

* Browser Extension
    * Detect jobs while browsing
    * Send current job to orchestrator
    * Show match score
    * Show missing requirements
    * Save job with one action

* Import / Export
    * Import application history
    * Export applications and analytics
    * Backup/restore local database

* Notifications
    * New high-match opportunities
    * Recruiter responses
    * Interview reminders
    * Application status changes

* Multi-profile Support
    * Different CVs
    * Different career objectives
    * Different technology preferences
    * Different geographic constraints