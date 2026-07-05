package com.example.interview.room.dto;

import com.example.interview.room.domain.InterviewRoomStatus;
import com.example.interview.room.domain.ParticipantRole;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MyRoomResponse {

    private final Long roomId;
    private final String title;
    private final InterviewRoomStatus status;
    private final ParticipantRole role;
    private final LocalDateTime createdAt;

    public MyRoomResponse(Long roomId, String title, InterviewRoomStatus status, ParticipantRole role, LocalDateTime createdAt) {
        this.roomId = roomId;
        this.title = title;
        this.status = status;
        this.role = role;
        this.createdAt = createdAt;
    }
}
