package com.example.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Redis에 캐시되는 채팅 메시지. 이전에는 common-module에 있어 chat-service(발행)와
 * chat-consumer(소비)가 공유했다. common 제거로 이곳으로 옮겼다.
 *
 * <p><b>주의:</b> Redis JSON 직렬화(GenericJackson2JsonRedisSerializer)는 값에
 * 클래스 전체 이름(@class)을 함께 저장한다. 패키지를 옮겼으므로, chat-service가
 * 같은 클래스를 여전히 {@code com.example.common.dto}로 발행하면 역직렬화가
 * 어긋난다. 발행 측도 이 패키지에 맞춰야 한다. (개발 캐시는 TTL로 자연 소멸)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

    private Long roomId;
    private Long userId;
    private String userName;
    private String content;
    private LocalDateTime createdAt;

}
