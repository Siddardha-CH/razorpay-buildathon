package com.recoup.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.recoup.backend.model.ActionResult;
import com.recoup.backend.model.CaseStatus;
import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.Contact;
import com.recoup.backend.model.Decision;
import com.recoup.backend.model.Diagnosis;
import com.recoup.backend.model.EventType;
import com.recoup.backend.model.InterventionType;
import com.recoup.backend.model.Promise;
import com.recoup.backend.model.RecoveryCase;
import com.recoup.backend.model.RevenueEvent;

/** Regression test for a real bug: CaseController used to page first and filter
 *  by status second, so a status filter only ever looked inside one page's
 *  worth of rows instead of across the whole batch. findByBatchIdAndStatus
 *  pushes the filter into the query itself, so pagination and filtering
 *  compose correctly regardless of page size. */
@SpringBootTest
class RecoveryCaseRepositoryTest {

    @Autowired
    private RecoveryCaseRepository caseRepository;
    @Autowired
    private RevenueEventRepository eventRepository;

    private RecoveryCase caseWithStatus(String batchId, String eventId, CaseStatus status) {
        RevenueEvent event = new RevenueEvent();
        event.setId(eventId);
        event.setContentKey(eventId);
        event.setType(EventType.FAILED_PAYMENT);
        event.setMerchantId("m");
        event.setCustomerId("c");
        event.setCustomerName("n");
        event.setContact(new Contact("+910000000000", "n@example.com", true, false));
        event.setAmountPaise(1000);
        event.setCurrency("INR");
        event.setCreatedAt("2026-08-29T10:00:00+05:30");
        eventRepository.save(event);

        RecoveryCase recoveryCase = new RecoveryCase();
        recoveryCase.setEvent(event);
        recoveryCase.setBatchId(batchId);
        recoveryCase.setStatus(status);
        recoveryCase.setDiagnosis(new Diagnosis(CauseCategory.TRANSIENT_RETRYABLE, 1.0, "r", "rule_engine"));
        recoveryCase.setDecision(new Decision(InterventionType.RETRY_PAYMENT, "r", 0, 0.5, 500));
        recoveryCase.setActionResult(new ActionResult(true, "d", 0, null));
        recoveryCase.setPromise(new Promise(0, null, null));
        recoveryCase.setProcessedAt("2026-08-29T10:00:00+05:30");
        return caseRepository.save(recoveryCase);
    }

    @Test
    void statusFilterSeesEveryMatchingRowNotJustTheFirstPage() {
        String batchId = "batch_repo_test_" + System.nanoTime();
        // 3 RECOVERED rows spread across what will be two size-2 pages, plus 2 PENDING rows.
        caseWithStatus(batchId, "evt_r1_" + batchId, CaseStatus.RECOVERED);
        caseWithStatus(batchId, "evt_p1_" + batchId, CaseStatus.PENDING);
        caseWithStatus(batchId, "evt_r2_" + batchId, CaseStatus.RECOVERED);
        caseWithStatus(batchId, "evt_p2_" + batchId, CaseStatus.PENDING);
        caseWithStatus(batchId, "evt_r3_" + batchId, CaseStatus.RECOVERED);

        Page<RecoveryCase> firstPage = caseRepository.findByBatchIdAndStatus(batchId, CaseStatus.RECOVERED, PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements())
            .as("total must reflect all matching rows in the batch, not just what fits on one page")
            .isEqualTo(3);
    }
}
