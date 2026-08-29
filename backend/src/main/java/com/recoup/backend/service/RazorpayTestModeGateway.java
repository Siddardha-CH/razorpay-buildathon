package com.recoup.backend.service;

import org.json.JSONObject;

import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.recoup.backend.model.ActionResult;
import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.RevenueEvent;

/** Calls the real Razorpay test-mode Payment Links API. Creating a link is the
 *  concrete recovery action for both a retry and an alternate-method nudge --
 *  whether the customer actually pays resolves later via webhook, which is out
 *  of scope for a batch demo run (see docs/ARCHITECTURE.md, "Extension points").
 *  This gateway only activates when RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET are
 *  configured; see GatewayConfig. */
public class RazorpayTestModeGateway implements PaymentGateway {

    private final RazorpayClient client;

    public RazorpayTestModeGateway(String keyId, String keySecret) throws RazorpayException {
        this.client = new RazorpayClient(keyId, keySecret);
    }

    @Override
    public String name() {
        return "razorpay_test_mode";
    }

    private ActionResult createLink(RevenueEvent event, String purpose) {
        try {
            JSONObject request = new JSONObject();
            request.put("amount", event.getAmountPaise());
            request.put("currency", event.getCurrency());
            request.put("description", "Recoup " + purpose + " for " + event.getId());

            JSONObject customer = new JSONObject();
            customer.put("name", event.getCustomerName());
            customer.put("contact", event.getContact().getPhone());
            customer.put("email", event.getContact().getEmail());
            request.put("customer", customer);

            JSONObject notify = new JSONObject();
            notify.put("sms", true);
            notify.put("email", true);
            request.put("notify", notify);
            request.put("reference_id", event.getId());

            PaymentLink link = client.paymentLink.create(request);
            String shortUrl = link.get("short_url");
            String id = link.get("id");
            return new ActionResult(true, "[LIVE test-mode] payment link created, awaiting customer action: " + shortUrl, 0, id);
        } catch (RazorpayException e) {
            return new ActionResult(false, "[LIVE test-mode] payment link creation failed: " + e.getMessage(), 0, null);
        }
    }

    @Override
    public ActionResult retryPayment(RevenueEvent event, CauseCategory category) {
        return createLink(event, "retry");
    }

    @Override
    public ActionResult createPaymentLink(RevenueEvent event, CauseCategory category) {
        return createLink(event, "alt-payment-method");
    }
}
