package com.example.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.kafka", "com.example.common"})
public class ChatConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatConsumerApplication.class, args);
    }
}
