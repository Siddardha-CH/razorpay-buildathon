package com.recoup.backend.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

/** The processed outcome of one RevenueEvent: its diagnosis, the intervention
 *  decided, the action actually executed, and any promise-to-pay captured. */
@Entity
public class RecoveryCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "event_id", referencedColumnName = "id")
    private RevenueEvent event;

    @Enumerated(EnumType.STRING)
    private CaseStatus status;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "reasoning", column = @Column(name = "diagnosis_reasoning", length = 512))
    })
    private Diagnosis diagnosis;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "reasoning", column = @Column(name = "decision_reasoning", length = 512))
    })
    private Decision decision;

    @Embedded
    private ActionResult actionResult;

    @Embedded
    private Promise promise;

    private String batchId;
    private String processedAt;

    public RecoveryCase() {
    }

    public Long getId() {
        return id;
    }

    public RevenueEvent getEvent() {
        return event;
    }

    public void setEvent(RevenueEvent event) {
        this.event = event;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public Diagnosis getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(Diagnosis diagnosis) {
        this.diagnosis = diagnosis;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public ActionResult getActionResult() {
        return actionResult;
    }

    public void setActionResult(ActionResult actionResult) {
        this.actionResult = actionResult;
    }

    public Promise getPromise() {
        return promise;
    }

    public void setPromise(Promise promise) {
        this.promise = promise;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(String processedAt) {
        this.processedAt = processedAt;
    }
}
