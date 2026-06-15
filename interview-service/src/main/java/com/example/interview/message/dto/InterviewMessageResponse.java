package com.example.interview.message.dto;

import com.example.interview.message.domain.InterviewMessage;
import com.example.interview.message.domain.InterviewMessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewMessageResponse {

    private Long messageId;
    private Long roomId;
    private Long senderId;
    private String senderName;
    private String senderPicture;
    private InterviewMessageType messageType;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    public static InterviewMessageResponse from(InterviewMessage message) {
        return new InterviewMessageResponse(
                message.getId(),
                message.getRoomId(),
                message.getSenderId(),
                message.getSenderName(),
                message.getSenderPicture(),
                message.getMessageType(),
                message.getContent(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }
}
