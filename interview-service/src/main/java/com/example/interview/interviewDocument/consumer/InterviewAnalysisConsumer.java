package com.example.interview.interviewDocument.consumer;

import com.example.interview.interviewDocument.domain.InterviewDocument;
import com.example.interview.interviewDocument.dto.KafkaAnalysisResult;
import com.example.interview.interviewDocument.repository.InterviewDocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewAnalysisConsumer {

    private final InterviewDocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "interview-analyzed",
            groupId = "interview-service-group"
    )
    @Transactional
    public void consume(String payload) {

        try {
            KafkaAnalysisResult result = objectMapper.readValue(payload, KafkaAnalysisResult.class);

            log.info("[Kafka Consumer] 객체 변환 성공 - RoomID: {}, DocID: {}", result.getRoomId(), result.getDocumentId());

            // 1. 카프카로 받아온 documentId를 이용해 기존 DB에 저정해둔 Document 에니팉 조회
            InterviewDocument document = documentRepository.findById(Long.parseLong(result.getDocumentId()))
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문서 ID입니다."));

            // 2. AI 분석 결과 항목돌을 DB컬럼에 업데이트 (JPA 더티체킹 활용)
            document.updateAiFeedback(
                    result.getPythonicScore(),
                    result.getCodeCritique(),
                    result.getTimeComplexity(),
                    result.getEdgeCase(),
                    result.getFollowUpQuestions()
            );
        } catch (JsonProcessingException e) {
            log.error("[Kafka Consumer] JSON 역직렬화 실패: {}", e.getMessage());

        } catch (Exception e) {
            log.error("[Kafka Consumer] 비즈니스 로직 처리 중 예외 발생", e);
        }
    }
}
