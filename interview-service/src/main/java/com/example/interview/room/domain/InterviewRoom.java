package com.example.interview.room.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewRoom {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String title; // 면접방 제목

    private Long createdBy; // 면접방 만든 유저 id

    private String inviteCode; // 초대 코드

    @Lob
    @Column(columnDefinition = "TEXT")
    private String problemContent; // 면접 문제

    @Enumerated(EnumType.STRING)
    private InterviewRoomStatus status; // 면접 상태

    private LocalDateTime scheduledAt; // 예약 시간
    private LocalDateTime startedAt; // 면접 시작 시간
    private LocalDateTime endedAt; // 면접 종료 시간
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; // 업데이트 시간

    public InterviewRoom(String title, String problemContent, Long createdBy, String inviteCode, LocalDateTime scheduledAt) {
        this.title = title;
        this.problemContent = problemContent;
        this.createdBy = createdBy;
        this.inviteCode = inviteCode;
        this.scheduledAt = scheduledAt;
        this.status = InterviewRoomStatus.WAITING;
        this.createdAt = LocalDateTime.now();
    }

    public static InterviewRoom create(
            String title,
            String problemContent,
            Long createdBy,
            String inviteCode,
            LocalDateTime createdAt
    ) {
        return new InterviewRoom(title, problemContent, createdBy, inviteCode, createdAt);
    }

    public void start() {
//        validateStatus(InterviewRoomStatus.WAITING);
        this.status = InterviewRoomStatus.START;
        this.updatedAt = LocalDateTime.now();
    }


    public void end() {
//        validateStatus(InterviewRoomStatus.IN_PROGRESS);
        this.status = InterviewRoomStatus.ENDED;
        this.endedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isCreater(Long userId) {
        return this.createdBy != null && this.createdBy.equals(userId);
    }

    public boolean isEnded() {
        return this.status == InterviewRoomStatus.ENDED;
    }

    private void validateStatus(InterviewRoomStatus requiredStatus) {
        if (this.status != requiredStatus) {
            throw new IllegalStateException("Invalid interview room status. current: " + this.status + ", required: " + requiredStatus);

        };
    }
}
