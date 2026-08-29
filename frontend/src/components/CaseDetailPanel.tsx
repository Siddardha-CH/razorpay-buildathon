import type { CaseDetail } from "../lib/types";
import { formatRupees, formatPercent, titleCase } from "../lib/format";

export function CaseDetailPanel({ detail, onClose }: { detail: CaseDetail | null; onClose: () => void }) {
  if (!detail) return null;

  return (
    <div className="drawer-backdrop" onClick={onClose}>
      <div className="drawer" onClick={(e) => e.stopPropagation()}>
        <div className="drawer__header">
          <div>
            <h2>{detail.customerName}</h2>
            <span className="drawer__subtitle">
              {detail.eventId} &middot; {titleCase(detail.eventType)}
            </span>
          </div>
          <button className="drawer__close" onClick={onClose} aria-label="Close">
            &times;
          </button>
        </div>

        <section className="drawer__section">
          <h4>Diagnosis</h4>
          <p>
            <strong>{titleCase(detail.diagnosisCategory)}</strong> ({formatPercent(detail.diagnosisConfidence)}{" "}
            confidence, {detail.diagnosisSource === "llm_assisted" ? "LLM-assisted narration" : "rule engine"})
          </p>
          <p className="drawer__muted">{detail.diagnosisReasoning}</p>
        </section>

        <section className="drawer__section">
          <h4>Decision</h4>
          <p>
            <strong>{titleCase(detail.intervention)}</strong> &middot; est. cost {formatRupees(detail.estimatedCostPaise)}{" "}
            &middot; p(recover) {formatPercent(detail.estimatedRecoveryProbability)}
          </p>
          <p className="drawer__muted">{detail.decisionReasoning}</p>
        </section>

        <section className="drawer__section">
          <h4>Action taken</h4>
          <p>{detail.actionDetail}</p>
          <p className="drawer__muted">Recovered: {formatRupees(detail.recoveredAmountPaise)}</p>
        </section>

        {detail.hasPromise && (
          <section className="drawer__section">
            <h4>Promise to pay</h4>
            <p>
              Promised {formatRupees(detail.promisedAmountPaise)} by {detail.promisedByDate} &mdash;{" "}
              {detail.promiseKept ? "kept" : "broken"}
            </p>
          </section>
        )}

        <section className="drawer__section">
          <h4>Audit trail</h4>
          <ol className="audit-trail">
            {detail.auditTrail.map((entry, idx) => (
              <li key={idx} className="audit-trail__entry">
                <div className="audit-trail__stage">
                  {titleCase(entry.stage)} <span className="audit-trail__timestamp">{entry.timestamp}</span>
                </div>
                <div className="audit-trail__detail">{entry.detail}</div>
                {entry.guardrails.length > 0 && (
                  <ul className="guardrail-list">
                    {entry.guardrails.map((g) => (
                      <li key={g.name} className={`guardrail ${g.passed ? "guardrail--pass" : "guardrail--fail"}`}>
                        <span>{g.passed ? "PASS" : "FAIL"}</span> {titleCase(g.name)} &mdash; {g.detail}
                      </li>
                    ))}
                  </ul>
                )}
              </li>
            ))}
          </ol>
        </section>
      </div>
    </div>
  );
}
