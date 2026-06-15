package com.example.kafka.consumer;


import com.example.common.dto.ChatMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class RedisConsumer {

    private final RedisTemplate<String, ChatMessageDto> chatRedisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "chat-events",
            groupId = "redis-group"
    )
    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltTopicSuffix = "-dlt"
    )
    public void consume(String payload) {
        System.out.println("kafka로 부터 받아온 메시지: " + payload);

        try {
            ChatMessageDto message = objectMapper.readValue(payload, ChatMessageDto.class);

            String key = "chat:room:" + message.getRoomId();

            // Redis 저장
            chatRedisTemplate.opsForList().rightPush(key, message); // 오른쪽에 push - 시간 순서 유지
            chatRedisTemplate.opsForList().trim(key, -100, -1); // 최신 메시지 100개만 남김

        } catch (Exception e) {
            log.error("Redis 저장 실패 payload: {}", payload, e);
            throw new RuntimeException("Redis 저장 실패", e);
        }

        System.out.println("Redis 저장 성공!");
    }
}
