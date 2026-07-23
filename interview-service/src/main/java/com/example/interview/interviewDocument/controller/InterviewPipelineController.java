package com.example.interview.interviewDocument.controller;

import com.example.interview.interviewDocument.service.InterviewPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/interview/room")
@RequiredArgsConstructor
public class InterviewPipelineController {

    private final InterviewPipelineService interviewPipelineService;

    @PostMapping("/{roomId}/end")
    public ResponseEntity<String> endInterview(
            @PathVariable Long roomId
    ) {
        log.info("[Controller] 면접 종료 파이프라인 API 호출 수신: RoomID: {}", roomId);

        try {
            interviewPipelineService.endInterviewPipeline(roomId);

            return ResponseEntity.ok("면접 세션 종료 및 AI 역량 채점 파이프라인 실행 성공");
        } catch (IllegalArgumentException e) {
            log.error("[Controller] 잘못된 요청 매개변수 에러: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IllegalStateException e) {
            log.error("[Controller] 방 상태 정합성 위반 에러: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            log.error("[Controller] 시스템 내부 치명적 파이프라인 에러 발생: ", e);
            return ResponseEntity.internalServerError().body("서버 내부 오류로 파이프라인 구동에 실패했습니다.");
        }
    }
}
