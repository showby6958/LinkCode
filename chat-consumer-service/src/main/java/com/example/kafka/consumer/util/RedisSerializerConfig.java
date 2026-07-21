package com.example.kafka.consumer.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 직렬화 빈. 이전에는 common-module이 제공했고, common 제거로 이곳으로 옮겼다.
 * ChatRedisConfig가 이 두 빈(String / Object)을 주입받는다.
 */
@Configuration
public class RedisSerializerConfig {

    @Bean
    public RedisSerializer<String> stringRedisSerializer() {
        return new StringRedisSerializer();
    }

    @Bean
    public RedisSerializer<Object> jsonRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }
}
