# Job Application Orchestrator

A local-first, agentic job discovery and application automation platform. Runs entirely on your machine — no cloud accounts, no subscriptions, no tracking.

---

## What It Does

- **Discovers jobs** from multiple public APIs (Remotive, Arbeitnow, Jobicy) and scores them against your candidate profile
- **Tracks applications** through their full lifecycle: Discovered → Applied → Interviewing → Offer / Rejected
- **Dispatches applications** automatically to job boards that accept direct submissions
- **Syncs recruiter emails** via a built-in SMTP server (route your email to `localhost:2525`, no OAuth required)
- **Generates cover letters** tailored to each job based on your skills and match score
- **Talks to an AI agent** (local Ollama or cloud API) for job analysis, keyword extraction, and reasoning
- **Extracts skills from your PDF resume** using Apache PDFBox + a dynamic keyword dictionary

---

## Architecture

Clean Hexagonal Architecture (Ports & Adapters) — pure Java 21, zero frameworks, zero annotations.

```
core/
  domain/          ← Business entities (CandidateProfile, JobOpportunity, MatchResult, ...)
  ports/in/        ← Use case interfaces (inbound)
  ports/out/       ← Repository & provider interfaces (outbound)
  service/         ← Domain services (DefaultDiscoverJobsService, DefaultAgentOrchestratorService, ...)

adapters/
  in/web/          ← HTTP handlers (DashboardHttpServer, REST API endpoints)
  in/email/        ← MiniMX SMTP server (port 2525)
  in/cli/          ← Agent CLI runner
  out/persistence/ ← JSON file repositories (data/*.json)
  out/jobsource/   ← Job source adapters (Remotive, Arbeitnow, Jobicy)
  out/llm/         ← LLM provider adapters (Ollama, Cloud API)
  out/ingestor/    ← PDF ingestion (PDFBox)

runtime/
  Bootstrap.java   ← Main entry point
```

---

## Quick Start

### Requirements

- Java 21+
- Maven 3.9+

```bash
git clone https://github.com/watashi-00/Job-Application-Orchestrator.git
cd Job-Application-Orchestrator
mvn package -DskipTests
java -jar target/application-orchestrator-1.0-SNAPSHOT.jar
```

Open **http://localhost:8080** in your browser.

---

## First Run

On first start, the app:
1. Fetches jobs from Remotive, Arbeitnow, and Jobicy APIs
2. Scores each job against a default candidate profile
3. Persists everything to `data/` (JSON files, no database required)

Upload your own PDF resume via the **Profile** section to replace the default profile.

---

## Uploading Your Resume

1. Open `http://localhost:8080`
2. In the left panel → click **Upload Resume PDF**
3. Select your PDF — skills, seniority, and work mode are extracted automatically
4. Edit extracted skills, target seniorities, and preferred work modes inline

---

## Email Inbox Sync (Optional)

The app runs a lightweight SMTP server on port `2525`. Route your recruiter emails here to automatically correlate them with your job applications.

```bash
# Test it with a local email
curl --url "smtp://localhost:2525" \
  --mail-from "recruiter@company.com" \
  --mail-rcpt "you@localhost" \
  --upload-file - <<EOF
Subject: Interview Invitation

Hi, we'd love to schedule an interview!
EOF
```

For Gmail forwarding: **Settings → Forwarding → Add forwarding address** → `your-alias@your-mx-domain` → route to `localhost:2525` via local MX record.

---

## AI Agent (Ollama)

The agent works with any local Ollama model or cloud API.

### Setup Ollama

```bash
# Install: https://ollama.com/download
ollama pull llama3.2
```

Open the dashboard → **Settings → LLM Config**:
- Provider: `ollama`
- Ollama URL: `http://localhost:11434`
- Click **Sync Models** → select your model
- Tier: `SMALL` (100 words/chunk), `MEDIUM` (500), `LARGE` (full doc)

### Agent CLI

```bash
java -jar target/application-orchestrator-1.0-SNAPSHOT.jar agent chat "Which jobs match my Java skills?"
java -jar target/application-orchestrator-1.0-SNAPSHOT.jar agent models
java -jar target/application-orchestrator-1.0-SNAPSHOT.jar agent config get
```

---

## REST API

Full OpenAPI 3.0 spec: [`docs/api/openapi.json`](docs/api/openapi.json)

| Endpoint | Description |
|---|---|
| `GET /api/jobs` | All discovered jobs with match scores |
| `POST /api/jobs` | Trigger job discovery |
| `POST /api/jobs/status` | Update job status |
| `POST /api/jobs/batch-status` | Batch status update |
| `GET /api/profile` | Get candidate profile |
| `POST /api/profile` | Upload resume PDF |
| `GET /api/profile/resume` | View resume PDF inline |
| `POST /api/profile/update` | Update profile fields |
| `GET /api/cover-letter?jobId=` | Generate cover letter |
| `POST /api/applications/dispatch` | Dispatch application |
| `GET /api/applications/logs` | Application dispatch logs |
| `GET /api/inbox` | Recruiter email inbox |
| `GET /api/tags` | Custom filter tags |
| `POST /api/tags` | Create custom tag |
| `GET /api/logs` | Unified system activity log |
| `POST /api/agent/chat` | Agent chat message |
| `GET /api/agent/config` | Get LLM config |
| `POST /api/agent/config` | Update LLM config |
| `GET /api/agent/models` | List available LLM models |

---

## Data Storage

All data is persisted locally in `data/`:

| File | Contents |
|---|---|
| `data/profile.json` | Candidate profile and preferences |
| `data/resume.pdf` | Uploaded resume PDF |
| `data/jobs.json` | Discovered jobs and match results |
| `data/applications-history.json` | Application status history |
| `data/emails-inbox.json` | Recruiter email inbox |
| `data/credentials-store.json` | Site credentials for auto-dispatch |
| `data/custom-tags.json` | User-defined filter tags |
| `data/agent-config.json` | LLM provider configuration |
| `data/skills-dictionary.json` | Skill keyword dictionary |

---

## Running Tests

```bash
mvn test
```

186 unit tests, 0 dependencies on external services.

---

## Project Goals

> Automate the mechanical parts of job hunting — discovery, tracking, applying — so you can focus on the parts that actually require you.

This is a local-first tool. It does not:
- Send your data to any cloud service
- Require accounts or API keys to discover jobs
- Store credentials anywhere except your local `data/` directory

---

## Roadmap

See [`docs/superpowers/plans/`](docs/superpowers/plans/) for the full implementation plan.

**Next (`feat/discovery-intelligence`):**
- GitHub-sourced skill keyword dictionary with alias matching
- Description-aware matching engine (score keywords in job description text)
- Configurable job discovery scheduler (every N minutes, quiet hours)
- Multi-page UI restructuring (`/jobs`, `/applications`, `/profile`, `/inbox`, `/agent`, `/settings`)
- Design system with constrained color tokens
- Frontend/backend separation (static files from classpath)
- Rich text job description rendering (HTML + Markdown)
- API, CLI, and Agent documentation

---

## License

MIT
