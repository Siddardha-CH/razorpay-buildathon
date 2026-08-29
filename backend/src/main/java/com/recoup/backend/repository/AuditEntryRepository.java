package com.recoup.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.recoup.backend.model.AuditEntry;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long> {

    List<AuditEntry> findByEventIdOrderByTimestampAsc(String eventId);

    List<AuditEntry> findByCaseIdOrderByTimestampAsc(Long caseId);

    List<AuditEntry> findByCaseIdIn(List<Long> caseIds);
}
