package com.recoup.backend.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.recoup.backend.model.Contact;
import com.recoup.backend.model.EventType;
import com.recoup.backend.model.RevenueEvent;

/** Produces a batch of synthetic revenue-loss events spanning the three loss
 *  modes named in the brief: payment failures, checkout abandonment, and
 *  overdue receivables (incl. failed subscription mandates). Decline codes are
 *  modeled on Razorpay's public error taxonomy (BAD_REQUEST_ERROR / GATEWAY_ERROR
 *  reason strings). */
@Service
public class SyntheticEventGenerator {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    private static final String[] FIRST_NAMES = {
        "Aarav", "Vivaan", "Ishaan", "Diya", "Ananya", "Kabir", "Riya", "Arjun",
        "Priya", "Rohan", "Meera", "Karan", "Sanya", "Aditya", "Neha", "Vikram"
    };
    private static final String[] LAST_NAMES = {
        "Sharma", "Verma", "Iyer", "Nair", "Reddy", "Gupta", "Khan", "Das", "Patel", "Rao"
    };

    private static final String[][] RETRYABLE_DECLINES = {
        {"GATEWAY_ERROR", "issuer_unavailable"},
        {"GATEWAY_ERROR", "bank_server_down"},
        {"BAD_REQUEST_ERROR", "insufficient_funds"}
    };
    private static final String[][] HARD_DECLINES = {
        {"BAD_REQUEST_ERROR", "card_expired"},
        {"BAD_REQUEST_ERROR", "invalid_card_number"},
        {"GATEWAY_ERROR", "authentication_failed"}
    };
    private static final String[][] FRAUD_DECLINES = {
        {"GATEWAY_ERROR", "risk_check_failed"},
        {"BAD_REQUEST_ERROR", "blocklisted_instrument"}
    };

    private static final String[] CHECKOUT_STAGES = {
        "otp_entry", "address_review", "payment_method_select", "order_review", "3ds_redirect"
    };
    private static final String[] PAYMENT_METHODS = {"upi", "card", "netbanking", "wallet", "emi"};
    private static final String[] ABANDON_NOTES = {
        "customer dropped after seeing delivery charges",
        "OTP did not arrive, customer gave up",
        "compared price with another app mid-checkout",
        null
    };

    public List<RevenueEvent> generateBatch(int n, long seed, Instant now) {
        Random rng = new Random(seed);
        List<RevenueEvent> events = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            EventType type = pickEventType(rng);
            String eventId = String.format("evt_%s_%04d", seed, i);
            Instant createdAt = now.minusSeconds(60L * 60 * (1 + rng.nextInt(240)));
            Contact contact = randomContact(rng);
            String name = FIRST_NAMES[rng.nextInt(FIRST_NAMES.length)] + " " + LAST_NAMES[rng.nextInt(LAST_NAMES.length)];

            RevenueEvent event = new RevenueEvent();
            event.setId(eventId);
            event.setType(type);
            event.setMerchantId("merchant_demo_01");
            event.setCustomerId("cust_" + i);
            event.setCustomerName(name);
            event.setContact(contact);
            event.setCurrency("INR");
            event.setCreatedAt(ISO.format(createdAt.atOffset(ZoneOffset.ofHoursMinutes(5, 30))));

            switch (type) {
                case FAILED_PAYMENT -> fillFailedPayment(event, rng);
                case ABANDONED_CHECKOUT -> fillAbandonedCheckout(event, rng);
                case FAILED_MANDATE -> fillFailedMandate(event, rng);
                case OVERDUE_RECEIVABLE -> fillOverdueReceivable(event, rng, now);
            }
            events.add(event);
        }
        return events;
    }

    private EventType pickEventType(Random rng) {
        double r = rng.nextDouble();
        if (r < 0.40) return EventType.FAILED_PAYMENT;
        if (r < 0.65) return EventType.ABANDONED_CHECKOUT;
        if (r < 0.85) return EventType.FAILED_MANDATE;
        return EventType.OVERDUE_RECEIVABLE;
    }

    private Contact randomContact(Random rng) {
        String phone = "+91" + (7000000000L + (long) (rng.nextDouble() * 2999999999L));
        String email = "user" + (1000 + rng.nextInt(9000)) + "@example.com";
        boolean whatsapp = rng.nextDouble() > 0.2;
        boolean dnd = rng.nextDouble() < 0.08;
        return new Contact(phone, email, whatsapp, dnd);
    }

    private void fillFailedPayment(RevenueEvent event, Random rng) {
        double r = rng.nextDouble();
        String[][] bucket = r < 0.55 ? RETRYABLE_DECLINES : (r < 0.85 ? HARD_DECLINES : FRAUD_DECLINES);
        String[] pick = bucket[rng.nextInt(bucket.length)];
        event.setAmountPaise(19900 + (long) (rng.nextDouble() * (4999900 - 19900)));
        event.setPaymentMethod(PAYMENT_METHODS[rng.nextInt(PAYMENT_METHODS.length)]);
        event.setDeclineCode(pick[0]);
        event.setDeclineReason(pick[1]);
        event.setAttemptCount(rng.nextInt(3));
    }

    private void fillAbandonedCheckout(RevenueEvent event, Random rng) {
        event.setAmountPaise(29900 + (long) (rng.nextDouble() * (2999900 - 29900)));
        event.setPaymentMethod(PAYMENT_METHODS[rng.nextInt(PAYMENT_METHODS.length)]);
        event.setCheckoutStage(CHECKOUT_STAGES[rng.nextInt(CHECKOUT_STAGES.length)]);
        event.setSupportNote(ABANDON_NOTES[rng.nextInt(ABANDON_NOTES.length)]);
    }

    private void fillFailedMandate(RevenueEvent event, Random rng) {
        String[][] bucket = rng.nextDouble() < 0.6 ? RETRYABLE_DECLINES : HARD_DECLINES;
        String[] pick = bucket[rng.nextInt(bucket.length)];
        event.setAmountPaise(9900 + (long) (rng.nextDouble() * (199900 - 9900)));
        event.setPaymentMethod("upi");
        event.setDeclineCode(pick[0]);
        event.setDeclineReason(pick[1]);
        event.setAttemptCount(rng.nextInt(4));
    }

    private void fillOverdueReceivable(RevenueEvent event, Random rng, Instant now) {
        int daysOverdue = 1 + rng.nextInt(75);
        boolean isB2b = rng.nextDouble() < 0.6;
        long amount = isB2b
            ? 500000 + (long) (rng.nextDouble() * (25000000 - 500000))
            : 50000 + (long) (rng.nextDouble() * (500000 - 50000));
        event.setAmountPaise(amount);
        event.setDaysOverdue(daysOverdue);
        event.setB2b(isB2b);
        Instant due = now.minusSeconds(86400L * daysOverdue);
        event.setInvoiceDueDate(ISO.format(due.atOffset(ZoneOffset.ofHoursMinutes(5, 30))));
    }
}
