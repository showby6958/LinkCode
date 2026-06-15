package com.example.auth.util;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@AutoConfigureAfter
@RequiredArgsConstructor
public class AuthRedisConfig {

    private final RedisConnectionFactory connectionFactory;
    private final RedisSerializer<String> stringRedisSerializer;

    // Auth -> <String, String>
    @Bean
    public RedisTemplate<String, String> authRedisTemplate() {

        RedisTemplate<String, String> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(stringRedisSerializer);

        return template;
    }
}
