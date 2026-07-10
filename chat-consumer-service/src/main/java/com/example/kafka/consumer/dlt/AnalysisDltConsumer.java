package com.example.kafka.consumer.DltConsumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class AnalysisDltConsumer {

    @KafkaListener(
            topics = "interview-analysis-dlt",
            groupId = "analysis-dlt-group"
    )
    public void consume(String payload) {

        System.out.println("대충 AI분석 이벤트 Dlt 받음");
    }
}
