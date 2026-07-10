package com.example.kafka.consumer.dlt.notify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class SlackNotifier {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${slack.webhook.url:}")
    private String webhookUrl;

    public void send(String message) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[Slack] webhook url이 설정되지 않아 알림을 생략합니다: {}", message);
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("text", message), headers);
            restTemplate.postForEntity(webhookUrl, request, String.class);
        } catch (Exception e) {
            // Slack 알림 실패가 DLT 컨슈머 자체를 멈추게 하면 안 되므로 재시도 없이 로그만 남긴다.
            log.error("[Slack] 알림 전송 실패: {}", message, e);
        }
    }
}
