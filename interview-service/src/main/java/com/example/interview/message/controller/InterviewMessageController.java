package com.example.interview.message.controller;

import com.example.interview.message.dto.InterviewMessageRequest;
import com.example.interview.message.dto.InterviewMessageResponse;
import com.example.interview.message.service.InterviewMessageService;
import com.example.interview.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

        // 채널 인터셉터가 LoginUser를 STOMP principal로 세팅해 둔다.
        LoginUser user = (LoginUser) principal;

        InterviewMessageResponse response = messageService.sendMessage(
                roomId,
                user.userId(),
                user.userName(),
                user.picture(),
                request
        );

        messagingTemplate.convertAndSend("/topic/interview/room/" + roomId + "/messages", response);
    }
}
