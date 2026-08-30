import { useState } from "react";
import { getCaseDetail, getMetrics, listCases, runBatch } from "./lib/api";
import type { CaseDetail, CaseSummary, Metrics } from "./lib/types";
import { ControlBar } from "./components/ControlBar";
import { KpiTiles } from "./components/KpiTiles";
import { BreakdownBars } from "./components/BreakdownBars";
import { CasesTable } from "./components/CasesTable";
import { CaseDetailPanel } from "./components/CaseDetailPanel";

export default function App() {
  const [batchId, setBatchId] = useState<string | null>(null);
  const [gatewayMode, setGatewayMode] = useState<string | null>(null);
  const [metrics, setMetrics] = useState<Metrics | null>(null);
  const [cases, setCases] = useState<CaseSummary[]>([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [selectedCase, setSelectedCase] = useState<CaseDetail | null>(null);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleRun(size: number, seed: number, apiKey: string) {
    setRunning(true);
    setError(null);
    try {
      const batch = await runBatch(size, seed, apiKey);
      setBatchId(batch.batchId);
      setGatewayMode(batch.gatewayMode);
      const [m, c] = await Promise.all([getMetrics(batch.batchId), listCases(batch.batchId)]);
      setMetrics(m);
      setCases(c);
      setStatusFilter("ALL");
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setRunning(false);
    }
  }

  async function handleStatusFilterChange(status: string) {
    setStatusFilter(status);
    if (!batchId) return;
    const filtered = await listCases(batchId, status === "ALL" ? undefined : status);
    setCases(filtered);
  }

  async function handleSelectCase(id: number) {
    const detail = await getCaseDetail(id);
    setSelectedCase(detail);
  }

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>Recoup</h1>
          <p className="app__tagline">AI revenue recovery agent &mdash; Razorpay AI Buildathon, Track 3</p>
        </div>
      </header>

      <ControlBar onRun={handleRun} running={running} gatewayMode={gatewayMode} />

      {error && <div className="error-banner">{error}</div>}

      {metrics && (
        <>
          <KpiTiles metrics={metrics} />
          <div className="breakdown-grid">
            <BreakdownBars
              title="Recovered vs at-risk, by root cause"
              rows={metrics.byCause.map((c) => ({
                label: c.category,
                count: c.count,
                primaryPaise: c.atRiskAmountPaise,
                recoveredPaise: c.recoveredAmountPaise,
              }))}
            />
            <BreakdownBars
              title="Recovered vs at-risk, by intervention"
              rows={metrics.byIntervention.map((i) => ({
                label: i.intervention,
                count: i.count,
                primaryPaise: i.atRiskAmountPaise,
                recoveredPaise: i.recoveredAmountPaise,
              }))}
            />
          </div>
          <CasesTable
            cases={cases}
            statusFilter={statusFilter}
            onStatusFilterChange={handleStatusFilterChange}
            onSelect={handleSelectCase}
          />
        </>
      )}

      {!metrics && !running && (
        <div className="empty-state">Run a recovery batch to generate synthetic at-risk revenue and see it recovered.</div>
      )}

      <CaseDetailPanel detail={selectedCase} onClose={() => setSelectedCase(null)} />
    </div>
  );
}
