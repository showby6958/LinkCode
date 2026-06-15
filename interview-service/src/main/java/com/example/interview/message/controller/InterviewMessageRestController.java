package com.example.interview.message.controller;

import com.example.common.security.CustomPrincipal;
import com.example.interview.message.dto.InterviewMessageResponse;
import com.example.interview.message.service.InterviewMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class InterviewMessageRestController {

    private final InterviewMessageService messageService;

    @GetMapping("/{roomId}/history")
    public ResponseEntity<List<InterviewMessageResponse>> getMessageHistory(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomPrincipal user
    ) {
        log.info("채팅 히스토리 조회 요청. roomId: {}, userId: {}", roomId, user.getUserId());

        List<InterviewMessageResponse> history = messageService.getMessageHistory(roomId, user.getUserId());

        log.info("채팅 히스토리 조회 성공.");
        return ResponseEntity.ok(history);
    }
}
