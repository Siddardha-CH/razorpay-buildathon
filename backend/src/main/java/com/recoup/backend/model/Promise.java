package com.recoup.backend.model;

import jakarta.persistence.Embeddable;

/** A verbal payment commitment captured during a human-escalated collections
 *  follow-up. All fields are null/zero when no promise applies to a case. */
@Embeddable
public class Promise {

    private long promisedAmountPaise;
    private String promisedByDate;
    private Boolean kept;

    public Promise() {
    }

    public Promise(long promisedAmountPaise, String promisedByDate, Boolean kept) {
        this.promisedAmountPaise = promisedAmountPaise;
        this.promisedByDate = promisedByDate;
        this.kept = kept;
    }

    public long getPromisedAmountPaise() {
        return promisedAmountPaise;
    }

    public String getPromisedByDate() {
        return promisedByDate;
    }

    public Boolean getKept() {
        return kept;
    }

    public boolean exists() {
        return promisedByDate != null;
    }
}
