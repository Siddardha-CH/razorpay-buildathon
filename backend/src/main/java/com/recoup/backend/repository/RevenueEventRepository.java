package com.recoup.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.recoup.backend.model.RevenueEvent;

public interface RevenueEventRepository extends JpaRepository<RevenueEvent, String> {
}
