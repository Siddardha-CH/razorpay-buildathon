package com.recoup.backend.service;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.RevenueEvent;

/** Generates the Hinglish outbound call script a human collections agent (or a
 *  future voice bot) would read for a human-escalated receivable -- the
 *  "Hinglish voice recovery" direction, scoped to what a hackathon build can
 *  responsibly ship: actual telephony/TTS is out of scope, but the script the
 *  call would follow is a real, usable artifact on its own.
 *
 *  Groq only phrases the script; it never decides whether to call, who to
 *  call, or what to offer -- that's still PolicyEngine's job. If Groq is
 *  unavailable, a deterministic Hinglish template stands in, so this runs
 *  fully offline too. */
@Service
public class CallScriptService {

    private final GroqClient groqClient;

    public CallScriptService(GroqClient groqClient) {
        this.groqClient = groqClient;
    }

    public String generateHinglishCallScript(RevenueEvent event, CauseCategory category) {
        String amount = String.format(Locale.forLanguageTag("en-IN"), "%,.2f", event.getAmountPaise() / 100.0);
        String urgency = category == CauseCategory.RECEIVABLE_LEGAL_STAGE ? "firm but respectful" : "warm and friendly";

        String prompt = String.format(
            "Write a short Hinglish (Hindi+English mixed, Roman script only, no Devanagari) phone call "
                + "script for a collections agent. Customer name: %s. Amount overdue: Rs.%s. Days overdue: %d. "
                + "Tone should be %s. Structure: brief greeting, state the reason for the call, ask for a specific "
                + "payment date commitment, polite closing. 3-4 sentences total, plain text, no headings or labels.",
            event.getCustomerName(), amount, event.getDaysOverdue(), urgency
        );

        return groqClient.complete(prompt, 180).orElseGet(() -> fallbackScript(event, amount));
    }

    private String fallbackScript(RevenueEvent event, String amount) {
        return "Namaste " + event.getCustomerName() + " ji, main aapke account ke regarding call kar raha hoon. "
            + "Aapka Rs." + amount + " ka payment " + event.getDaysOverdue() + " din se pending hai. "
            + "Kya aap bata sakte hain ki is amount ko aap kab tak clear kar payenge? "
            + "Hum aapki madad ke liye yahan hain, thoda sa update mil jaaye toh accha rahega. Dhanyavaad.";
    }
}
