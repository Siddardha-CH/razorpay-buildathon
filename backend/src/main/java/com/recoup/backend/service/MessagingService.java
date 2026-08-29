package com.recoup.backend.service;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.InterventionType;
import com.recoup.backend.model.RevenueEvent;

/** Outbound copy rendering. Sending is always simulated (logged, never actually
 *  dispatched) -- this is a batch-demo pipeline, not a real messaging integration. */
@Service
public class MessagingService {

    public String channelFor(InterventionType intervention) {
        return switch (intervention) {
            case SEND_ALT_PAYMENT_LINK, SEND_REMINDER_EMAIL -> "email";
            case SEND_REMINDER_SMS -> "sms";
            case SEND_REMINDER_WHATSAPP -> "whatsapp";
            default -> "none";
        };
    }

    public String render(RevenueEvent event, InterventionType intervention, CauseCategory category, String link) {
        String amount = String.format(Locale.forLanguageTag("en-IN"), "%,.2f", event.getAmountPaise() / 100.0);
        String name = event.getCustomerName();

        if (intervention == InterventionType.RETRY_PAYMENT) {
            return "Hi " + name + ", your payment of Rs." + amount + " didn't go through due to a temporary "
                + "issue. We're retrying automatically -- no action needed.";
        }
        if (intervention == InterventionType.SEND_ALT_PAYMENT_LINK) {
            boolean hinglish = event.getSupportNote() != null && event.getSupportNote().toLowerCase(Locale.ROOT).contains("hindi");
            if (hinglish) {
                return "Namaste " + name + ", aapka Rs." + amount + " ka payment complete nahi ho paya. "
                    + "Kripya is link se dobara try karein: " + link;
            }
            return "Hi " + name + ", we couldn't complete your payment of Rs." + amount
                + ". Please use this secure link to try another method: " + link;
        }
        if (category == CauseCategory.RECEIVABLE_GENTLE_STAGE) {
            return "Hi " + name + ", a friendly reminder that Rs." + amount + " is due on your invoice. Pay now: " + link;
        }
        if (category == CauseCategory.RECEIVABLE_ESCALATION_STAGE || category == CauseCategory.RECEIVABLE_LEGAL_STAGE) {
            return "Hi " + name + ", your payment of Rs." + amount + " is now " + event.getDaysOverdue()
                + " days overdue. Please settle at the earliest to avoid service interruption: " + link;
        }
        return "Hi " + name + ", please complete your pending payment of Rs." + amount + ": " + link;
    }
}
