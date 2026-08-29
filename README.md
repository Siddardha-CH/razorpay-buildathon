# Recoup — AI Revenue Recovery Agent

Built for the **Razorpay AI Buildathon**, Track 3: AI Revenue Recovery.

> Find revenue that's slipping away and win it back — payment failures, checkout
> abandonment, and overdue receivables, closed end to end with a bounded, audited,
> compliant recovery workflow.

## Why this track

Tracks 1 and 2 (Agentic Commerce, Risk Manager) are the ones most applicants will
reach for first. Revenue Recovery is explicitly named in the brief as needing
builders who can chain diagnosis → decision → execution into one accountable loop,
and it's the track where "how much money did you actually get back, and can you
prove you didn't do anything shady to get it" is a single, honest number rather
than a vibe. That's a good fit for a backend-and-data-shaped build: a policy engine
you can unit test line by line, a real (test-mode) payment gateway integration, and
a dashboard that shows its work.

## What it does

Recoup ingests a batch of at-risk revenue events — failed payments, abandoned
checkouts, failed subscription mandates, overdue B2B/B2C receivables — and for each
one:

1. **Diagnoses** the root cause (rule-based; an LLM only narrates *why* for
   ambiguous checkout abandonment, it never decides).
2. **Decides** a bounded intervention through a compliance-first policy engine:
   retry, send an alternate payment link, send a reminder, escalate to a human, or
   route to the risk team — with every decision checked against five guardrails
   (no DND contact, no contact during quiet hours 9pm–8am IST, max 3 auto-retries,
   a 60-day automated-pursuit ceiling, and cost bounded by expected recovered value).
3. **Acts**: creates a real Razorpay test-mode Payment Link when credentials are
   configured, or a seeded, reproducible simulation otherwise. Every send is logged,
   never actually dispatched.
4. **Tracks promise-to-pay** commitments for human-escalated B2B receivables.

Every step writes an audit entry — the diagnosis reasoning, the decision reasoning,
every guardrail evaluated and whether it passed — queryable per case from the
dashboard. The batch-level metrics report exactly what the brief asks for: money
recovered, an honest breakdown by cause and channel, and a guardrail-violation count
that should always read zero (see `RecoveryPipelineServiceTest`, which asserts this
over a 200-event batch).

## Tech stack

- **Backend**: Java 17, Spring Boot 3.3, Spring Data JPA, H2 (file-based, zero
  setup) with a MySQL profile for a real deployment, JUnit 5 + AssertJ.
- **Payment gateway**: `razorpay-java` against the real Payment Links API in test
  mode, behind a `PaymentGateway` interface with a deterministic simulator fallback.
- **LLM**: Groq's OpenAI-compatible chat completions endpoint (`java.net.http.HttpClient`,
  no extra client dependency), used only for narrating ambiguous checkout-abandonment
  reasons — optional, the whole pipeline runs and is tested with zero API keys.
- **Frontend**: React 19 + TypeScript (Vite), no charting library — the breakdown
  bars are plain CSS/DOM so there's no third-party API surface to defend that
  wasn't actually needed.

## Running it

**Backend** (from `backend/`):

```bash
mvn spring-boot:run
```

Starts on `http://localhost:8080`. Uses a local H2 file database at `backend/data/`
by default — nothing to install. To point at MySQL instead:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

To use the real Razorpay test-mode gateway instead of the simulator, set
`RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` (from your Razorpay test-mode dashboard)
before starting. To enable LLM-assisted abandonment narration, set `GROQ_API_KEY`.
Both are optional — the pipeline is fully functional and fully tested without either.

**Frontend** (from `frontend/`):

```bash
npm install
npm run dev
```

Opens on `http://localhost:5173`. Set `VITE_API_BASE_URL` if the backend isn't on
`localhost:8080`.

**Tests** (from `backend/`):

```bash
mvn test
```

15 tests: guardrail-by-guardrail unit tests on the policy engine, diagnosis
rule-mapping tests, gateway-determinism tests, and one full-batch integration
smoke test that asserts zero guardrail violations across 200 generated events.

**Demo flow**: start the backend, start the frontend, open the dashboard, enter
the API key (`dev-local-key` by default — see `RECOUP_API_KEY` to change it),
click "Run recovery batch", and click into any case to see its full diagnosis →
decision → action → audit trail.

## What broke, and how I got out

The metrics endpoint threw `LazyInitializationException` the first time I ran it
against a fresh batch: `AuditEntry.guardrails` is a Hibernate `@ElementCollection`
(lazy by default), and `MetricsService.forBatch()` was reading it in a stream
*after* the repository call had already returned and the session had closed. It
worked fine for a single case fetched through the controller (that request is
wrapped in one transaction end to end) but broke the moment I aggregated across a
whole batch outside of any explicit transaction boundary. Fixed by marking
`MetricsService.forBatch()` `@Transactional(readOnly = true)` so the collection
loads while the session is still open. Caught it because
`RecoveryPipelineServiceTest` runs the full batch-then-metrics path as one
integration test — a unit test mocking the repository would have missed it, since
the mock would've just handed back an already-populated in-memory list.

## Assumptions worth flagging

- Recovery probabilities per cause category (`PolicyEngine.RECOVERY_PROBABILITY`)
  are documented, reasonable-looking assumptions, not measured historical rates —
  called out explicitly in code and in the metrics, per the track's "honest
  metrics" bar.
- The simulator resolves an outcome (paid / not paid) synchronously so a batch run
  is demoable in seconds. The real Razorpay gateway instead creates a genuine
  pending Payment Link — resolving whether the customer actually paid requires a
  webhook listener, which is out of scope for a batch-demo CLI/dashboard (see
  `docs/ARCHITECTURE.md`, Extension points).
- Promise-to-pay keep/break outcomes are likewise simulated (seeded, reproducible),
  standing in for a real collections CRM.

## Repo layout

```
backend/    Spring Boot API (pipeline, policy engine, gateway, persistence)
frontend/   React + TypeScript dashboard
docs/       Architecture documentation
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the data flow, entity
design, and guardrail rationale.
