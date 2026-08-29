package com.recoup.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ActionResult {

    private boolean success;

    @Column(length = 1024)
    private String detail;

    private long recoveredAmountPaise;
    private String providerRef;

    public ActionResult() {
    }

    public ActionResult(boolean success, String detail, long recoveredAmountPaise, String providerRef) {
        this.success = success;
        this.detail = detail;
        this.recoveredAmountPaise = recoveredAmountPaise;
        this.providerRef = providerRef;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getDetail() {
        return detail;
    }

    public long getRecoveredAmountPaise() {
        return recoveredAmountPaise;
    }

    public String getProviderRef() {
        return providerRef;
    }
}
