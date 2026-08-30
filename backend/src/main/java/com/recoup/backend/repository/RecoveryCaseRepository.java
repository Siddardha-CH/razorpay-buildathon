package com.recoup.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.recoup.backend.model.CaseStatus;
import com.recoup.backend.model.RecoveryCase;

public interface RecoveryCaseRepository extends JpaRepository<RecoveryCase, Long> {

    Page<RecoveryCase> findByBatchId(String batchId, Pageable pageable);

    Page<RecoveryCase> findByBatchIdAndStatus(String batchId, CaseStatus status, Pageable pageable);

    List<RecoveryCase> findByBatchId(String batchId);
}
