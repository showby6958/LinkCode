package com.example.kafka.consumer.dlt;

import com.example.kafka.consumer.dlt.domain.FailedEventLog;
import com.example.kafka.consumer.dlt.notify.SlackNotifier;
import com.example.kafka.consumer.dlt.repository.FailedEventLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisDltConsumer {

    private static final String TOPIC = "interview-analysis-dlt";
    private static final DateTimeFormatter FAILED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;
    private final FailedEventLogRepository failedEventLogRepository;
    private final SlackNotifier slackNotifier;

    // ai-consumer(Python)가 Gemini 분석을 3회 재시도까지 실패한 뒤 보내는 이벤트.
    // 재시도는 이미 ai-consumer 쪽에서 끝났으므로 여기서는 기록(DB)과 알림(Slack)만 수행한다.
    @KafkaListener(
            topics = TOPIC,
            groupId = "analysis-dlt-group"
    )
    public void consume(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode origin = node.path("originEvent");
            String reason = node.path("reason").asText("사유 미상");
            String failedAtRaw = node.path("failedAt").asText("");
            String roomId = origin.path("roomId").asText("?");
            String documentId = origin.path("documentId").asText("?");

            failedEventLogRepository.save(new FailedEventLog(TOPIC, payload, reason, parseFailedAt(failedAtRaw)));

            String message = String.format(
                    "*AI 분석 실패 (DLT)*%n> 면접방 ID: `%s` / 문서 ID: `%s`%n> 사유: %s%n> 실패 시각: %s",
                    roomId, documentId, reason, failedAtRaw
            );
            slackNotifier.send(message);

        } catch (Exception e) {
            // 파싱/저장 단계 오류로 컨슈머가 멈추면 안 되므로 로그만 남기고 다음 메시지를 계속 처리한다.
            log.error("[DLT] AI 분석 실패 이벤트 처리 중 오류. payload={}", payload, e);
        }
    }

    private LocalDateTime parseFailedAt(String raw) {
        try {
            return LocalDateTime.parse(raw, FAILED_AT_FORMAT);
        } catch (DateTimeParseException e) {
            return LocalDateTime.now();
        }
    }
}
