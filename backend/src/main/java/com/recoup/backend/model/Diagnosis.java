package com.recoup.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class Diagnosis {

    @Enumerated(EnumType.STRING)
    private CauseCategory category;

    private double confidence;

    @Column(length = 512)
    private String reasoning;

    /** "rule_engine" or "llm_assisted" -- money-relevant classification is always rule_engine;
     *  an LLM may only enrich the narration shown to a human. */
    private String source;

    public Diagnosis() {
    }

    public Diagnosis(CauseCategory category, double confidence, String reasoning, String source) {
        this.category = category;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.source = source;
    }

    public CauseCategory getCategory() {
        return category;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getReasoning() {
        return reasoning;
    }

    public String getSource() {
        return source;
    }
}
