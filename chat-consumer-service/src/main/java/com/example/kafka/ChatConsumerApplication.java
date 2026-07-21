package com.example.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// common-module 의존을 끊었으므로 com.example.kafka 만 스캔한다(기본 동작).
@SpringBootApplication
public class ChatConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatConsumerApplication.class, args);
    }
}
