package com.recoup.backend.service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recoup.backend.model.ActionResult;
import com.recoup.backend.model.AuditEntry;
import com.recoup.backend.model.CaseStatus;
import com.recoup.backend.model.Decision;
import com.recoup.backend.model.Diagnosis;
import com.recoup.backend.model.EventType;
import com.recoup.backend.model.GuardrailCheck;
import com.recoup.backend.model.InterventionType;
import com.recoup.backend.model.Promise;
import com.recoup.backend.model.RecoveryCase;
import com.recoup.backend.model.RevenueEvent;
import com.recoup.backend.repository.AuditEntryRepository;
import com.recoup.backend.repository.RecoveryCaseRepository;
import com.recoup.backend.repository.RevenueEventRepository;

/** Orchestrates the full per-event pipeline: diagnose -> decide -> act ->
 *  promise-to-pay, writing one audit entry per stage (with every guardrail
 *  evaluated) and persisting the resulting RecoveryCase. */
@Service
public class RecoveryPipelineService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    private final DiagnosisService diagnosisService;
    private final PolicyEngine policyEngine;
    private final ActionExecutionService actionExecutionService;
    private final PromiseToPayService promiseToPayService;
    private final RevenueEventRepository eventRepository;
    private final RecoveryCaseRepository caseRepository;
    private final AuditEntryRepository auditEntryRepository;

    public RecoveryPipelineService(DiagnosisService diagnosisService, PolicyEngine policyEngine,
                                    ActionExecutionService actionExecutionService, PromiseToPayService promiseToPayService,
                                    RevenueEventRepository eventRepository, RecoveryCaseRepository caseRepository,
                                    AuditEntryRepository auditEntryRepository) {
        this.diagnosisService = diagnosisService;
        this.policyEngine = policyEngine;
        this.actionExecutionService = actionExecutionService;
        this.promiseToPayService = promiseToPayService;
        this.eventRepository = eventRepository;
        this.caseRepository = caseRepository;
        this.auditEntryRepository = auditEntryRepository;
    }

    @Transactional
    public String runBatch(List<RevenueEvent> events, ZonedDateTime now) {
        String batchId = "batch_" + UUID.randomUUID().toString().substring(0, 8);
        for (RevenueEvent event : events) {
            processOne(event, batchId, now);
        }
        return batchId;
    }

    private void processOne(RevenueEvent event, String batchId, ZonedDateTime now) {
        eventRepository.save(event);

        RecoveryCase recoveryCase = new RecoveryCase();
        recoveryCase.setEvent(event);
        recoveryCase.setStatus(CaseStatus.PENDING);
        recoveryCase.setBatchId(batchId);
        recoveryCase = caseRepository.save(recoveryCase); // assigns the id used to link audit entries below
        Long caseId = recoveryCase.getId();

        Diagnosis diagnosis = diagnosisService.diagnose(event);
        recordAudit(event.getId(), caseId, "diagnose",
            "category=" + diagnosis.getCategory() + " (" + diagnosis.getSource() + "): " + diagnosis.getReasoning(), now, List.of());

        Decision decision = policyEngine.decide(event, diagnosis.getCategory(), now);
        List<GuardrailCheck> guardrails = policyEngine.allGuardrails(
            event, decision.getIntervention(), now, decision.getEstimatedCostPaise(), decision.getExpectedValuePaise());
        recordAudit(event.getId(), caseId, "decide", decision.getReasoning(), now, guardrails);

        ActionResult actionResult = actionExecutionService.execute(event, diagnosis.getCategory(), decision);
        recordAudit(event.getId(), caseId, "act", actionResult.getDetail(), now, List.of());

        Promise promise = new Promise(0, null, null);
        if (decision.getIntervention() == InterventionType.HUMAN_ESCALATION && event.getType() == EventType.OVERDUE_RECEIVABLE) {
            promise = promiseToPayService.maybeRecordPromise(event, diagnosis.getCategory(), now);
            if (promise.exists()) {
                recordAudit(event.getId(), caseId, "promise_to_pay",
                    "promised Rs." + (promise.getPromisedAmountPaise() / 100.0) + " by " + promise.getPromisedByDate()
                        + ", kept=" + promise.getKept(), now, List.of());
                if (Boolean.TRUE.equals(promise.getKept())) {
                    actionResult = new ActionResult(true, actionResult.getDetail() + " | promise kept, payment received",
                        promise.getPromisedAmountPaise(), actionResult.getProviderRef());
                }
            }
        }

        recoveryCase.setStatus(resolveStatus(decision.getIntervention(), actionResult, promise));
        recoveryCase.setDiagnosis(diagnosis);
        recoveryCase.setDecision(decision);
        recoveryCase.setActionResult(actionResult);
        recoveryCase.setPromise(promise);
        recoveryCase.setProcessedAt(ISO.format(now.withZoneSameInstant(ZoneOffset.ofHoursMinutes(5, 30))));
        caseRepository.save(recoveryCase);
    }

    private CaseStatus resolveStatus(InterventionType intervention, ActionResult actionResult, Promise promise) {
        return switch (intervention) {
            case ROUTE_TO_RISK_TEAM -> CaseStatus.ROUTED_TO_RISK;
            case MARK_DO_NOT_CONTACT, STOP_PURSUIT -> CaseStatus.STOPPED;
            case DEFER_QUIET_HOURS -> CaseStatus.PENDING;
            case HUMAN_ESCALATION -> Boolean.TRUE.equals(promise.getKept()) ? CaseStatus.RECOVERED : CaseStatus.ESCALATED;
            default -> actionResult.getRecoveredAmountPaise() > 0 ? CaseStatus.RECOVERED : CaseStatus.PENDING;
        };
    }

    private void recordAudit(String eventId, Long caseId, String stage, String detail, ZonedDateTime now, List<GuardrailCheck> guardrails) {
        AuditEntry entry = new AuditEntry(eventId, caseId, stage, detail,
            ISO.format(now.withZoneSameInstant(ZoneOffset.ofHoursMinutes(5, 30))));
        entry.setGuardrails(guardrails);
        auditEntryRepository.save(entry);
    }
}
