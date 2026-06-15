package com.example.common.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void publish(String eventType, String key, String payload) {

        kafkaTemplate.send(eventType, key, payload).
                whenComplete((result, ex) -> {
                    if (ex != null) {
                        // 로그 + 예외 처리 (relay에서 재시도 유도)
                        throw new RuntimeException("kafka publish 실패", ex);
                    }
                });
    }
}
