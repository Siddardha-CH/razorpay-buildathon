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
   retry, schedule a spaced-out mandate retry, send an alternate payment link, send
   a reminder, escalate to a human, or route to the risk team — with every decision
   checked against five guardrails (no DND contact, no contact during quiet hours
   9pm–8am IST, max 3 auto-retries, a 60-day automated-pursuit ceiling, and cost
   bounded by expected recovered value, where the recovery probability behind that
   last check comes from a logistic regression model trained on the system's own
   past outcomes — see "Machine learning" below).
3. **Acts**: creates a real Razorpay test-mode Payment Link when credentials are
   configured, or a seeded, reproducible simulation otherwise. Every send is logged,
   never actually dispatched. Human-escalated receivables get a generated Hinglish
   call script for the collections agent.
4. **Tracks promise-to-pay** commitments for human-escalated B2B receivables.

Every step writes an audit entry — the diagnosis reasoning, the decision reasoning,
every guardrail evaluated and whether it passed — queryable per case from the
dashboard. The batch-level metrics report exactly what the brief asks for: money
recovered, an honest breakdown by cause and channel, and a guardrail-violation count
that should always read zero (see `RecoveryPipelineServiceTest`, which asserts this
over a 200-event batch).

## Coverage against the brief

The Track 3 page names seven example directions. Each one maps to an identifiable
piece of this codebase, not just a passing resemblance:

| Brief's example direction | Where it lives |
|---|---|
| Payment degradation → root cause → recovery action | `DiagnosisService` + `PolicyEngine` + `ActionExecutionService`, for `FAILED_PAYMENT` events |
| Checkout drop-off recovery | `ABANDONED_CHECKOUT` handling in `DiagnosisService` (LLM-narrated) + reminder intervention |
| Failed-subscription recovery | `FAILED_MANDATE` events, diagnosed the same as payments but decided differently |
| Mandate retry sequencer | `MandateRetrySequencer` — spaced retry cadence (+1/+3/+7 days) instead of an immediate same-pass retry |
| B2B receivables chaser | `OVERDUE_RECEIVABLE` escalation tiers (gentle → escalation → legal) + `HUMAN_ESCALATION` |
| Hinglish voice recovery | `CallScriptService` — generates the Hinglish call script a collections agent (or future voice bot) would read; voice synthesis itself is out of scope for this build |
| Promise-to-pay tracker | `PromiseToPayService` |

## Machine learning

Every guardrail check that weighs "is this intervention worth its cost" needs a
recovery probability. That number used to be a flat, hand-picked constant per
cause category — honest about being an assumption, but still a guess. It's now
a real logistic regression model (`LogisticRegressionModel`): hand-implemented
in plain Java (sigmoid + batch gradient descent + L2 regularization, no ML
library), trained on every past case where a recovery was actually attempted
and a terminal outcome observed — amount, days overdue, attempt count, B2B
flag, and cause category as features.

Deliberately **not** an LLM, and deliberately a model you can print the
coefficients of and defend line by line — the same reasoning as `PolicyEngine`
itself (see `docs/ARCHITECTURE.md`, "Why a policy engine instead of an LLM
here"): the model produces one interpretable number, PolicyEngine's guardrails
still make every actual decision.

- **Cold start**: below 30 observed outcomes, `MlRecoveryProbabilityEstimator`
  falls back to the same assumed constants as before (`AssumedRecoveryRates`) —
  it never pretends to have learned something it hasn't.
- **Retraining**: happens at the start of every batch run, on everything
  observed *before* that batch, so a batch never trains on its own outcomes.
- **Watch it learn**: the dashboard's "Recovery-probability model" panel shows
  the model's current per-category estimate next to the assumed constant it
  started from — run a couple of batches and watch them diverge as real
  (simulated) outcomes accumulate.
- **A bug this caught**: the first trained model predicted ~37% recovery for
  fraud-suspected cases despite zero training examples in that category (fraud
  is always routed to the risk team, never actively pursued) — its weight for
  that category sat at exactly zero, and the model quietly extrapolated a
  meaningless number from unrelated features instead. `PolicyEngine` itself was
  never affected (fraud's `ROUTE_TO_RISK_TEAM` path never consults the
  probability at all), but the model-status display was misleading. Now
  `FRAUD_SUSPECTED` is explicitly pinned to 0 regardless of training state —
  see `MlRecoveryProbabilityEstimatorTest.neverExtrapolatesANonZeroProbabilityForFraudSuspectedEvenAfterTraining`.

## Tech stack

- **Backend**: Java 17, Spring Boot 3.3, Spring Data JPA, H2 (file-based, zero
  setup) with a MySQL profile for a real deployment, JUnit 5 + AssertJ.
- **ML**: logistic regression, hand-implemented in plain Java (no ML library) —
  see "Machine learning" above.
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

37 tests: guardrail-by-guardrail unit tests on the policy engine (including the
mandate-retry-sequencer path), diagnosis rule-mapping tests, gateway-determinism
tests, the Hinglish call-script offline-fallback test, the ML model's own tests
(a held-out-accuracy check on synthetic separable data, the fraud-extrapolation
regression test, an end-to-end "does a real batch produce enough history to
train on" test), request-boundary/validation tests, and full-batch integration
tests — including one asserting zero guardrail violations across 200 generated
events and one asserting a same-seed batch recovers the identical amount every run.

**Demo flow**: start the backend, start the frontend, open the dashboard, enter
the API key (`dev-local-key` by default — see `RECOUP_API_KEY` to change it),
click "Run recovery batch" a couple of times, and: click into any case to see
its full diagnosis → decision → action → audit trail, and check the
"Recovery-probability model" panel to see the learned estimates diverge from
the assumed constants as more batches run.

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

Two more broke later, both self-inflicted by the same fix: making `RevenueEvent`
ids unique per batch (to stop re-running the same seed from colliding on the
database's primary key) accidentally made the *simulated recovery outcomes*
non-reproducible too, because the simulator and promise-to-pay service were
hashing that same salted id for their randomness. Same seed, same inputs,
different "luck" every run. Fixed by splitting row identity (`id`, salted per
batch) from a separate `contentKey` (derived from seed+index only, never salted)
that the RNG keys off instead — caught by testing the exact property a user
flagged when they noticed re-running the same seed gave different recovered
amounts.

A fourth broke after adding the ML model: the trained model predicted ~37%
recovery for fraud-suspected cases with zero training examples in that
category, extrapolating from unrelated features instead of admitting it had
never seen one. Caught by inspecting `/api/model/status` after two real batch
runs, not by a test written in advance — fixed by pinning `FRAUD_SUSPECTED` to
0 explicitly, then wrote the test afterward (see "Machine learning" above).

## Assumptions worth flagging

- Recovery probabilities per cause category (`AssumedRecoveryRates`) are
  documented, reasonable-looking assumptions, not measured historical rates —
  called out explicitly in code and in the metrics, per the track's "honest
  metrics" bar. They're also exactly what the ML model (see "Machine learning"
  above) is trying to learn past, given enough real outcomes.
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
