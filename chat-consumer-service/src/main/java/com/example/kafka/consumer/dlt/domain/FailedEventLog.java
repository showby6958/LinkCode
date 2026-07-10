package com.example.kafka.consumer.dlt.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FailedEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String topic;

    @Lob
    private String payload;

    private String reason;

    private LocalDateTime failedAt;

    private LocalDateTime loggedAt;

    public FailedEventLog(String topic, String payload, String reason, LocalDateTime failedAt) {
        this.topic = topic;
        this.payload = payload;
        this.reason = reason;
        this.failedAt = failedAt;
        this.loggedAt = LocalDateTime.now();
    }
}
