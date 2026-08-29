package com.recoup.backend.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class GuardrailCheck {

    private String name;
    private boolean passed;
    private String detail;

    public GuardrailCheck() {
    }

    public GuardrailCheck(String name, boolean passed, String detail) {
        this.name = name;
        this.passed = passed;
        this.detail = detail;
    }

    public String getName() {
        return name;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getDetail() {
        return detail;
    }
}
