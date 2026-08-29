import { formatRupees, titleCase } from "../lib/format";

export interface BreakdownRow {
  label: string;
  count: number;
  primaryPaise: number;
  recoveredPaise: number;
}

/** Simple dependency-free bar chart: a track scaled to the largest primary
 *  value in the set, with a recovered-amount segment overlaid. */
export function BreakdownBars({ title, rows }: { title: string; rows: BreakdownRow[] }) {
  const max = Math.max(1, ...rows.map((r) => r.primaryPaise));

  return (
    <div className="breakdown">
      <h3 className="breakdown__title">{title}</h3>
      <div className="breakdown__rows">
        {rows.map((row) => {
          const totalPct = (row.primaryPaise / max) * 100;
          const recoveredPct = row.primaryPaise === 0 ? 0 : (row.recoveredPaise / row.primaryPaise) * totalPct;
          return (
            <div className="breakdown__row" key={row.label}>
              <div className="breakdown__row-label">
                <span>{titleCase(row.label)}</span>
                <span className="breakdown__row-count">{row.count}</span>
              </div>
              <div className="breakdown__track">
                <div className="breakdown__bar breakdown__bar--total" style={{ width: `${totalPct}%` }} />
                <div className="breakdown__bar breakdown__bar--recovered" style={{ width: `${recoveredPct}%` }} />
              </div>
              <div className="breakdown__row-amounts">
                {formatRupees(row.recoveredPaise)} / {formatRupees(row.primaryPaise)}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
