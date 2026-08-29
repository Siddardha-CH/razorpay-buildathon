import type { BatchRunResponse, CaseDetail, CaseSummary, Metrics } from "./types";

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

async function handle<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const body = await response.text();
    throw new Error(`${response.status} ${response.statusText}: ${body}`);
  }
  return response.json() as Promise<T>;
}

export function runBatch(size: number, seed: number, apiKey: string): Promise<BatchRunResponse> {
  return fetch(`${BASE_URL}/api/batches/run?size=${size}&seed=${seed}`, {
    method: "POST",
    headers: { "X-API-Key": apiKey },
  }).then((r) => handle<BatchRunResponse>(r));
}

export function getMetrics(batchId: string): Promise<Metrics> {
  return fetch(`${BASE_URL}/api/metrics?batchId=${encodeURIComponent(batchId)}`).then((r) => handle<Metrics>(r));
}

export function listCases(batchId: string, status?: string, page = 0, size = 200): Promise<CaseSummary[]> {
  const params = new URLSearchParams({ batchId, page: String(page), size: String(size) });
  if (status) params.set("status", status);
  return fetch(`${BASE_URL}/api/cases?${params}`).then((r) => handle<CaseSummary[]>(r));
}

export function getCaseDetail(id: number): Promise<CaseDetail> {
  return fetch(`${BASE_URL}/api/cases/${id}`).then((r) => handle<CaseDetail>(r));
}
