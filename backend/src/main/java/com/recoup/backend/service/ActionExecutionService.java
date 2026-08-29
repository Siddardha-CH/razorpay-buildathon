package com.recoup.backend.service;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.recoup.backend.model.ActionResult;
import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.Decision;
import com.recoup.backend.model.InterventionType;
import com.recoup.backend.model.RevenueEvent;

/** Executes exactly one bounded action per case, per the decided intervention. */
@Service
public class ActionExecutionService {

    private static final Set<InterventionType> CONTACT_INTERVENTIONS = Set.of(
        InterventionType.SEND_ALT_PAYMENT_LINK, InterventionType.SEND_REMINDER_SMS,
        InterventionType.SEND_REMINDER_WHATSAPP, InterventionType.SEND_REMINDER_EMAIL
    );

    private final PaymentGateway gateway;
    private final MessagingService messagingService;

    public ActionExecutionService(PaymentGateway gateway, MessagingService messagingService) {
        this.gateway = gateway;
        this.messagingService = messagingService;
    }

    public ActionResult execute(RevenueEvent event, CauseCategory category, Decision decision) {
        InterventionType intervention = decision.getIntervention();

        if (intervention == InterventionType.RETRY_PAYMENT) {
            ActionResult result = gateway.retryPayment(event, category);
            String message = messagingService.render(event, intervention, category, "(auto-retry, no link)");
            return withDetail(result, message + " | " + result.getDetail());
        }

        if (CONTACT_INTERVENTIONS.contains(intervention)) {
            ActionResult result = gateway.createPaymentLink(event, category);
            String link = result.getProviderRef() != null ? result.getProviderRef() : "https://rzp.io/l/demo-link";
            String channel = messagingService.channelFor(intervention);
            String message = messagingService.render(event, intervention, category, link);
            String detail = "[SIMULATED SEND via " + channel + " to " + event.getContact().getPhone() + "] \""
                + message + "\" | gateway: " + result.getDetail();
            return withDetail(result, detail);
        }

        if (intervention == InterventionType.HUMAN_ESCALATION) {
            return new ActionResult(true, "[QUEUED] escalated " + event.getId() + " to collections/ops team for manual follow-up", 0, null);
        }
        if (intervention == InterventionType.ROUTE_TO_RISK_TEAM) {
            return new ActionResult(true, "[QUEUED] routed " + event.getId() + " to risk team for manual fraud review; no recovery action taken", 0, null);
        }
        if (intervention == InterventionType.MARK_DO_NOT_CONTACT) {
            return new ActionResult(true, "customer has DND flag set; no outbound action taken", 0, null);
        }
        if (intervention == InterventionType.DEFER_QUIET_HOURS) {
            return new ActionResult(true, "deferred to next contactable window (outside 9pm-8am IST)", 0, null);
        }
        if (intervention == InterventionType.STOP_PURSUIT) {
            return new ActionResult(true, "stopped: intervention cost exceeds expected recovery value", 0, null);
        }
        throw new IllegalStateException("unhandled intervention: " + intervention);
    }

    private ActionResult withDetail(ActionResult original, String detail) {
        return new ActionResult(original.isSuccess(), detail, original.getRecoveredAmountPaise(), original.getProviderRef());
    }
}
