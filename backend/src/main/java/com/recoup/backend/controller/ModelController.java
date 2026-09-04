package com.recoup.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recoup.backend.dto.CategoryProbabilityDto;
import com.recoup.backend.dto.ModelStatusDto;
import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.Contact;
import com.recoup.backend.model.RevenueEvent;
import com.recoup.backend.service.AssumedRecoveryRates;
import com.recoup.backend.service.MlRecoveryProbabilityEstimator;

/** Read-only window into the recovery-probability model: whether it's trained,
 *  how much history it learned from, and -- the interesting part for a demo --
 *  how its per-category estimate compares to the assumed constant it started
 *  from. */
@RestController
public class ModelController {

    private final MlRecoveryProbabilityEstimator estimator;

    public ModelController(MlRecoveryProbabilityEstimator estimator) {
        this.estimator = estimator;
    }

    @GetMapping("/api/model/status")
    public ModelStatusDto status() {
        List<CategoryProbabilityDto> byCategory = List.of(CauseCategory.values()).stream()
            .map(category -> new CategoryProbabilityDto(
                category.name(),
                AssumedRecoveryRates.forCategory(category),
                estimator.estimate(representativeEvent(category), category)
            ))
            .toList();
        return new ModelStatusDto(estimator.isTrained(), estimator.trainingSampleCount(), byCategory);
    }

    /** A typical-looking event for each category, purely so the status
     *  endpoint has something to feed the estimator for display -- not used
     *  anywhere in the actual pipeline. */
    private RevenueEvent representativeEvent(CauseCategory category) {
        RevenueEvent event = new RevenueEvent();
        event.setContact(new Contact("+910000000000", "rep@example.com", true, false));
        event.setAmountPaise(100_000); // Rs 1,000
        event.setAttemptCount(0);
        event.setB2b(false);
        event.setDaysOverdue(switch (category) {
            case RECEIVABLE_GENTLE_STAGE -> 7;
            case RECEIVABLE_ESCALATION_STAGE -> 30;
            case RECEIVABLE_LEGAL_STAGE -> 60;
            default -> 0;
        });
        return event;
    }
}
