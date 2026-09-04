export interface BatchRunResponse {
  batchId: string;
  totalEvents: number;
  gatewayMode: string;
  modelTrained: boolean;
  modelTrainingSamples: number;
}

export interface CategoryProbability {
  category: string;
  assumedProbability: number;
  currentEstimate: number;
}

export interface ModelStatus {
  trained: boolean;
  trainingSamples: number;
  byCategory: CategoryProbability[];
}

export interface CauseBreakdown {
  category: string;
  count: number;
  atRiskAmountPaise: number;
  recoveredAmountPaise: number;
}

export interface InterventionBreakdown {
  intervention: string;
  count: number;
  atRiskAmountPaise: number;
  costPaise: number;
  recoveredAmountPaise: number;
}

export interface Metrics {
  batchId: string;
  totalCases: number;
  atRiskAmountPaise: number;
  recoveredAmountPaise: number;
  recoveryRate: number;
  recoveredCount: number;
  pendingCount: number;
  stoppedCount: number;
  escalatedCount: number;
  routedToRiskCount: number;
  guardrailChecksEvaluated: number;
  guardrailViolations: number;
  stoppingRuleTriggers: number;
  humanEscalations: number;
  byCause: CauseBreakdown[];
  byIntervention: InterventionBreakdown[];
}

export interface CaseSummary {
  id: number;
  eventId: string;
  eventType: string;
  customerName: string;
  amountPaise: number;
  category: string;
  intervention: string;
  status: string;
  recoveredAmountPaise: number;
  processedAt: string;
}

export interface GuardrailCheck {
  name: string;
  passed: boolean;
  detail: string;
}

export interface AuditEntry {
  stage: string;
  detail: string;
  timestamp: string;
  guardrails: GuardrailCheck[];
}

export interface CaseDetail {
  id: number;
  eventId: string;
  eventType: string;
  customerName: string;
  customerPhone: string;
  amountPaise: number;
  b2b: boolean;
  daysOverdue: number;
  diagnosisCategory: string;
  diagnosisConfidence: number;
  diagnosisReasoning: string;
  diagnosisSource: string;
  intervention: string;
  decisionReasoning: string;
  estimatedCostPaise: number;
  estimatedRecoveryProbability: number;
  scheduledFor: string | null;
  actionSuccess: boolean;
  actionDetail: string;
  recoveredAmountPaise: number;
  providerRef: string | null;
  hasPromise: boolean;
  promisedAmountPaise: number;
  promisedByDate: string | null;
  promiseKept: boolean | null;
  status: string;
  processedAt: string;
  auditTrail: AuditEntry[];
}
