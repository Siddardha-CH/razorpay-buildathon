import type { Metrics } from "../lib/types";
import { formatPercent, formatRupees } from "../lib/format";

export function KpiTiles({ metrics }: { metrics: Metrics }) {
  const tiles = [
    { label: "Revenue at risk", value: formatRupees(metrics.atRiskAmountPaise), tone: "neutral" },
    { label: "Revenue recovered", value: formatRupees(metrics.recoveredAmountPaise), tone: "good" },
    { label: "Recovery rate", value: formatPercent(metrics.recoveryRate), tone: "good" },
    {
      label: "Guardrail violations",
      value: String(metrics.guardrailViolations),
      sub: `${metrics.guardrailChecksEvaluated} checks evaluated`,
      tone: metrics.guardrailViolations === 0 ? "good" : "bad",
    },
    { label: "Human escalations", value: String(metrics.humanEscalations), tone: "neutral" },
    { label: "Stopping-rule triggers", value: String(metrics.stoppingRuleTriggers), tone: "neutral" },
  ];

  return (
    <div className="kpi-grid">
      {tiles.map((tile) => (
        <div className={`kpi-tile kpi-tile--${tile.tone}`} key={tile.label}>
          <div className="kpi-tile__label">{tile.label}</div>
          <div className="kpi-tile__value">{tile.value}</div>
          {tile.sub && <div className="kpi-tile__sub">{tile.sub}</div>}
        </div>
      ))}
    </div>
  );
}
