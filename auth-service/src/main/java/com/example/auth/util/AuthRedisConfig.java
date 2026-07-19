package com.example.auth.util;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
public class AuthRedisConfig {

    private final RedisConnectionFactory connectionFactory;
    
    @Bean
    public RedisSerializer<String> stringRedisSerializer() {
        return new StringRedisSerializer();
    }

    // Auth -> <String, String>
    @Bean
    public RedisTemplate<String, String> authRedisTemplate() {

        RedisTemplate<String, String> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);

        RedisSerializer<String> serializer = stringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);

        return template;
    }
}
