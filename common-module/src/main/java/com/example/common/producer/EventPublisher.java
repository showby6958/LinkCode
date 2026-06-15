package com.example.common.producer;

public interface EventPublisher {

    void publish(String eventType, String key, String payload);
}
