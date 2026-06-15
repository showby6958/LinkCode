package com.example.interview.message.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        indexes = {
                @Index(
                        name = "idx_interview_message_room_created_at",
                        columnList = "room_id, created_at"
                )
        }
)
public class InterviewMessage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long roomId; // 면접방 id

    private Long senderId; // 메시지를 보낸 사용자 id
    private String senderName; // 메시지를 보낸 사용자 이름
    private String senderPicture; // 메시지를 보낸 사용자 프로필 사진 url

    @Enumerated(EnumType.STRING)
    private InterviewMessageType messageType;

    private String content; // 메시지 내용

    private LocalDateTime createdAt; // 메시지 생성 시간
    private LocalDateTime updatedAt; // 메시지 수정 시간

    public InterviewMessage(Long roomId, Long senderId, String senderName, String senderPicture, InterviewMessageType messageType, String content) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderPicture = senderPicture;
        this.messageType = messageType;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = null;
    }

    public static InterviewMessage chat(
            Long roomId,
            Long senderId,
            String senderName,
            String senderPicture,
            String content
    ) {
        if (senderId == null) {
            throw new IllegalArgumentException("senderId must not be null.");
        }

        return new InterviewMessage(
                roomId,
                senderId,
                senderName,
                senderPicture,
                InterviewMessageType.CHAT,
                content
        );
    }

    // 시스템 메시지 (senderId가 null)
    public static InterviewMessage system(
            Long roomId,
            String content
    ) {
        return new InterviewMessage(
                roomId,
                null,
                "SYSTEM",
                null,
                InterviewMessageType.SYSTEM,
                content
        );
    }

    public boolean isChat() {
        return this.messageType == InterviewMessageType.CHAT;
    }

    public boolean isSystem() {
        return this.messageType == InterviewMessageType.SYSTEM;
    }

    public boolean isSentBy(Long userId) {
        return this.senderId != null && this.senderId.equals(userId);
    }
}
