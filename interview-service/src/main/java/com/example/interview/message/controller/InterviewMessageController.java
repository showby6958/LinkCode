package com.example.interview.message.controller;

import com.example.common.security.CustomPrincipal;
import com.example.interview.message.dto.InterviewMessageRequest;
import com.example.interview.message.dto.InterviewMessageResponse;
import com.example.interview.message.service.InterviewMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class InterviewMessageController {

    private final InterviewMessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/interview/room/{roomId}/messages")
    public void sendMessage(
            @DestinationVariable Long roomId,
            InterviewMessageRequest request,
            Principal principal
    ) {
        log.info("웹소켓 메시지 받음. roomId={}, content={}",
                roomId,
                request.getContent());

        Authentication authentication = (Authentication) principal;
        CustomPrincipal user = (CustomPrincipal) authentication.getPrincipal();

        InterviewMessageResponse response = messageService.sendMessage(
                roomId,
                user.getUserId(),
                user.getUserName(),
                user.getPicture(),
                request
        );

        messagingTemplate.convertAndSend("/topic/interview/room/" + roomId + "/messages", response);
    }
}
