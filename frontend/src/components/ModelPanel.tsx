import type { ModelStatus } from "../lib/types";
import { formatPercent, titleCase } from "../lib/format";

export function ModelPanel({ status }: { status: ModelStatus }) {
  return (
    <div className="model-panel">
      <div className="model-panel__header">
        <h3>Recovery-probability model</h3>
        <span className={`model-badge ${status.trained ? "model-badge--trained" : "model-badge--cold-start"}`}>
          {status.trained ? `trained on ${status.trainingSamples} outcomes` : "cold start -- using assumed rates"}
        </span>
      </div>
      <p className="model-panel__note">
        Logistic regression, trained on this system's own past outcomes. Below 30 observed
        outcomes it falls back to the assumed constants; run more batches to watch the
        estimate move.
      </p>
      <table className="model-table">
        <thead>
          <tr>
            <th>Cause category</th>
            <th>Assumed</th>
            <th>Current estimate</th>
          </tr>
        </thead>
        <tbody>
          {status.byCategory.map((row) => (
            <tr key={row.category}>
              <td>{titleCase(row.category)}</td>
              <td>{formatPercent(row.assumedProbability)}</td>
              <td className={row.currentEstimate === row.assumedProbability ? "" : "model-table__learned"}>
                {formatPercent(row.currentEstimate)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
