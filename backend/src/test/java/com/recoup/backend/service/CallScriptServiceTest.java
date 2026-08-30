package com.recoup.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.RevenueEvent;
import com.recoup.backend.util.TestFixtures;

class CallScriptServiceTest {

    // No GROQ_API_KEY in the test environment, so this exercises the deterministic
    // Hinglish fallback -- the offline path the whole pipeline is tested against.
    private final CallScriptService callScriptService = new CallScriptService(new GroqClient());

    @Test
    void fallsBackToADeterministicHinglishScriptWithoutAnLlmKey() {
        RevenueEvent event = TestFixtures.overdueReceivable("evt_call", 50, true, 1_500_000);
        String script = callScriptService.generateHinglishCallScript(event, CauseCategory.RECEIVABLE_LEGAL_STAGE);

        assertThat(script).contains(event.getCustomerName());
        assertThat(script).contains("50");
        assertThat(script).containsIgnoringCase("dhanyavaad");
    }
}
