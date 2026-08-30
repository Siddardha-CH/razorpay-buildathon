package com.recoup.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class Decision {

    @Enumerated(EnumType.STRING)
    private InterventionType intervention;

    @Column(length = 512)
    private String reasoning;

    private long estimatedCostPaise;
    private double estimatedRecoveryProbability;
    private double expectedValuePaise;

    /** Set only for SCHEDULE_MANDATE_RETRY -- when the sequencer will next
     *  attempt this mandate. Null for every other intervention. */
    private String scheduledFor;

    public Decision() {
    }

    public Decision(InterventionType intervention, String reasoning, long estimatedCostPaise,
                     double estimatedRecoveryProbability, double expectedValuePaise) {
        this(intervention, reasoning, estimatedCostPaise, estimatedRecoveryProbability, expectedValuePaise, null);
    }

    public Decision(InterventionType intervention, String reasoning, long estimatedCostPaise,
                     double estimatedRecoveryProbability, double expectedValuePaise, String scheduledFor) {
        this.intervention = intervention;
        this.reasoning = reasoning;
        this.estimatedCostPaise = estimatedCostPaise;
        this.estimatedRecoveryProbability = estimatedRecoveryProbability;
        this.expectedValuePaise = expectedValuePaise;
        this.scheduledFor = scheduledFor;
    }

    public InterventionType getIntervention() {
        return intervention;
    }

    public String getReasoning() {
        return reasoning;
    }

    public long getEstimatedCostPaise() {
        return estimatedCostPaise;
    }

    public double getEstimatedRecoveryProbability() {
        return estimatedRecoveryProbability;
    }

    public double getExpectedValuePaise() {
        return expectedValuePaise;
    }

    public String getScheduledFor() {
        return scheduledFor;
    }
}
