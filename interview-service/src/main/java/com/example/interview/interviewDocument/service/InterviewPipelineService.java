package com.example.interview.interviewDocument.service;

import com.example.interview.interviewDocument.domain.InterviewDocument;
import com.example.interview.interviewDocument.dto.KafkaAnalysisResult;
import com.example.interview.interviewDocument.repository.InterviewDocumentRepository;
import com.example.interview.room.domain.InterviewRoom;
import com.example.interview.room.domain.InterviewRoomStatus;
import com.example.interview.room.repository.InterviewRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewPipelineService {

    private final InterviewRoomRepository roomRepository;
    private final InterviewDocumentRepository documentRepository;
    private final RedisTemplate<String, byte[]> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate; // 스냅샷(String) 조회용
    private final KafkaTemplate<String, KafkaAnalysisResult> kafkaTemplate;

    private static final String REDIS_KEY_PREFIX = "interview:room:";

    @Transactional
    public void endInterviewPipeline(Long roomId) {
        log.info("[Pipeline] 면접 종료 프로세스 개시 - RoomID: {}", roomId);

        InterviewRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 면접방입니다."));

        if (room.getStatus() == InterviewRoomStatus.ENDED) {
            throw new IllegalStateException("이미 종료된 면접방입니다.");
        }

        // 1. Redis에서 프론트가 업로드해둔 최신 snapshot(String)을 바로 수거
        String snapshotKey = REDIS_KEY_PREFIX + roomId + ":snapshot";
        String finalPlainTextCode = stringRedisTemplate.opsForValue().get(snapshotKey);

        if (finalPlainTextCode == null) {
            finalPlainTextCode = "# 면접이 종료되었습니다. (작성된 코드가 없습니다.)";
            log.warn("[Pipeline] Redis 스냅샷이 비어있습니다. 빈 상태로 처리합니다.");
        }

        // 2. 방 상태값 변경
        room.end();
        roomRepository.save(room);

        // 3. 별도의 document 엔티티 생성 및 저장
        InterviewDocument codeDocument = InterviewDocument.createCodeDocument(room, finalPlainTextCode);
        documentRepository.save(codeDocument);

        // 4. Kafka 이벤트 발행 (AI 분석 요청)
        KafkaAnalysisResult analysisEvent = KafkaAnalysisResult.builder()
                .roomId(room.getId())
                .documentId(String.valueOf(codeDocument.getId()))
                .codeContent(finalPlainTextCode)
                .problemContent(room.getProblemContent())
                .build();

        kafkaTemplate.send("interview-ended", String.valueOf(room.getId()), analysisEvent);
        log.info("[Pipeline] AI 분석 요청 카프카 이벤트 발행 완료 (RoomID: {})", roomId);

        // 5. 면접이 종료되었으므로 Redis 데이터 제거
        redisTemplate.delete(REDIS_KEY_PREFIX + roomId + ":yjs"); // Y.js 바이너리 리스트 삭제
        stringRedisTemplate.delete(REDIS_KEY_PREFIX + roomId + ":snapshot"); // 평문 스냅샷 삭제

        log.info("[Pipeline] Redis 리소스 반환 완료 및 파이프라인 종료 (RoomID: {})", roomId);
    }

}
