package com.recoup.backend.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

/** One append-only row per diagnosis / decision / action step. This table IS the
 *  audit trail the submission bar asks for: every guardrail evaluated for a step
 *  is attached here, pass or fail, so it can be inspected after the fact. */
@Entity
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventId;
    private Long caseId;
    private String stage;

    @Column(length = 1024)
    private String detail;

    private String timestamp;

    @ElementCollection
    @CollectionTable(name = "audit_entry_guardrails", joinColumns = @JoinColumn(name = "audit_entry_id"))
    private List<GuardrailCheck> guardrails = new ArrayList<>();

    public AuditEntry() {
    }

    public AuditEntry(String eventId, Long caseId, String stage, String detail, String timestamp) {
        this.eventId = eventId;
        this.caseId = caseId;
        this.stage = stage;
        this.detail = detail;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getStage() {
        return stage;
    }

    public String getDetail() {
        return detail;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public List<GuardrailCheck> getGuardrails() {
        return guardrails;
    }

    public void setGuardrails(List<GuardrailCheck> guardrails) {
        this.guardrails = guardrails;
    }
}
