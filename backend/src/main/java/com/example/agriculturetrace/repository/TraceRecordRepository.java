package com.example.agriculturetrace.repository;

import com.example.agriculturetrace.entity.TraceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 溯源访问记录仓库。
 */
public interface TraceRecordRepository extends JpaRepository<TraceRecord, String> {
}
