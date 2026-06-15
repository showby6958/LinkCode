package com.example.kafka.consumer.DltConsumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ChatEventDltConsumer {

    @KafkaListener(
            topics = "chat-events-dlt",
            groupId = "redis-group"
    )
    public void consume(String payload) {

        // ... 실제 로그 시스템 전송 로직...
        System.out.println("로그 시스템에 전송" + payload);

        // ... 알림 발송 ...
        System.out.println("Slack에 알림 발송");
    }
}
