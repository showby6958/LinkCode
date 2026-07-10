package com.example.kafka.consumer.dlt.repository;

import com.example.kafka.consumer.dlt.domain.FailedEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedEventLogRepository extends JpaRepository<FailedEventLog, Long> {
}
