package com.recoup.backend.controller;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.recoup.backend.dto.AuditEntryDto;
import com.recoup.backend.dto.CaseDetailDto;
import com.recoup.backend.dto.CaseSummaryDto;
import com.recoup.backend.model.CaseStatus;
import com.recoup.backend.model.RecoveryCase;
import com.recoup.backend.repository.AuditEntryRepository;
import com.recoup.backend.repository.RecoveryCaseRepository;

@RestController
public class CaseController {

    private final RecoveryCaseRepository caseRepository;
    private final AuditEntryRepository auditEntryRepository;

    public CaseController(RecoveryCaseRepository caseRepository, AuditEntryRepository auditEntryRepository) {
        this.caseRepository = caseRepository;
        this.auditEntryRepository = auditEntryRepository;
    }

    @GetMapping("/api/cases")
    public List<CaseSummaryDto> list(@RequestParam String batchId,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "50") int size) {
        Page<RecoveryCase> result = caseRepository.findByBatchId(batchId, PageRequest.of(page, size));
        return result.getContent().stream()
            .filter(c -> status == null || c.getStatus() == CaseStatus.valueOf(status))
            .map(CaseSummaryDto::from)
            .toList();
    }

    @GetMapping("/api/cases/{id}")
    public CaseDetailDto detail(@PathVariable Long id) {
        RecoveryCase recoveryCase = caseRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("no case with id " + id));
        List<AuditEntryDto> auditTrail = auditEntryRepository.findByCaseIdOrderByTimestampAsc(id).stream()
            .map(AuditEntryDto::from)
            .toList();
        return CaseDetailDto.from(recoveryCase, auditTrail);
    }
}
