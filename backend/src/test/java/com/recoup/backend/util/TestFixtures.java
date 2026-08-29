package com.recoup.backend.util;

import com.recoup.backend.model.Contact;
import com.recoup.backend.model.EventType;
import com.recoup.backend.model.RevenueEvent;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static RevenueEvent failedPayment(String id, String declineReason, int attemptCount, boolean dnd) {
        RevenueEvent event = new RevenueEvent();
        event.setId(id);
        event.setType(EventType.FAILED_PAYMENT);
        event.setMerchantId("merchant_demo_01");
        event.setCustomerId("cust_" + id);
        event.setCustomerName("Test Customer");
        event.setContact(new Contact("+919999999999", "test@example.com", true, dnd));
        event.setAmountPaise(500000);
        event.setCurrency("INR");
        event.setCreatedAt("2026-08-29T10:00:00+05:30");
        event.setPaymentMethod("upi");
        event.setDeclineCode("BAD_REQUEST_ERROR");
        event.setDeclineReason(declineReason);
        event.setAttemptCount(attemptCount);
        return event;
    }

    public static RevenueEvent overdueReceivable(String id, int daysOverdue, boolean b2b, long amountPaise) {
        RevenueEvent event = new RevenueEvent();
        event.setId(id);
        event.setType(EventType.OVERDUE_RECEIVABLE);
        event.setMerchantId("merchant_demo_01");
        event.setCustomerId("cust_" + id);
        event.setCustomerName("Test Debtor");
        event.setContact(new Contact("+919999999999", "test@example.com", true, false));
        event.setAmountPaise(amountPaise);
        event.setCurrency("INR");
        event.setCreatedAt("2026-08-29T10:00:00+05:30");
        event.setDaysOverdue(daysOverdue);
        event.setB2b(b2b);
        return event;
    }
}
