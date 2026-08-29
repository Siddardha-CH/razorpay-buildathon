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

    public Decision() {
    }

    public Decision(InterventionType intervention, String reasoning, long estimatedCostPaise,
                     double estimatedRecoveryProbability, double expectedValuePaise) {
        this.intervention = intervention;
        this.reasoning = reasoning;
        this.estimatedCostPaise = estimatedCostPaise;
        this.estimatedRecoveryProbability = estimatedRecoveryProbability;
        this.expectedValuePaise = expectedValuePaise;
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
}
