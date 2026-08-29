import type { CaseSummary } from "../lib/types";
import { formatRupees, titleCase } from "../lib/format";

const STATUS_OPTIONS = ["ALL", "RECOVERED", "PENDING", "ESCALATED", "STOPPED", "ROUTED_TO_RISK"];

export function CasesTable({
  cases,
  statusFilter,
  onStatusFilterChange,
  onSelect,
}: {
  cases: CaseSummary[];
  statusFilter: string;
  onStatusFilterChange: (status: string) => void;
  onSelect: (id: number) => void;
}) {
  return (
    <div className="cases-panel">
      <div className="cases-panel__header">
        <h3>Cases ({cases.length})</h3>
        <select value={statusFilter} onChange={(e) => onStatusFilterChange(e.target.value)}>
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {s === "ALL" ? "All statuses" : titleCase(s)}
            </option>
          ))}
        </select>
      </div>
      <div className="cases-table-wrap">
        <table className="cases-table">
          <thead>
            <tr>
              <th>Customer</th>
              <th>Event</th>
              <th>Cause</th>
              <th>Intervention</th>
              <th>Amount</th>
              <th>Recovered</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {cases.map((c) => (
              <tr key={c.id} onClick={() => onSelect(c.id)} className="cases-table__row">
                <td>{c.customerName}</td>
                <td>{titleCase(c.eventType)}</td>
                <td>{titleCase(c.category)}</td>
                <td>{titleCase(c.intervention)}</td>
                <td>{formatRupees(c.amountPaise)}</td>
                <td>{formatRupees(c.recoveredAmountPaise)}</td>
                <td>
                  <span className={`status-pill status-pill--${c.status.toLowerCase()}`}>{titleCase(c.status)}</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
