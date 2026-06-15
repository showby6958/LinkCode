package com.example.kafka.consumer.util;

import com.example.common.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
@RequiredArgsConstructor
public class ChatRedisConfig {

    private final RedisConnectionFactory connectionFactory;
    private final RedisSerializer<String> stringRedisSerializer;
    private final RedisSerializer<Object> jsonRedisSerializer;

    // Chat -> <String, ChatMessageDto>
    @Bean
    public RedisTemplate<String, ChatMessageDto> chatRedisTemplate() {

        RedisTemplate<String, ChatMessageDto> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jsonRedisSerializer);

        return template;
    }
}
