package com.example.interview.room.dto;

import com.example.interview.room.domain.ParticipantRole;
import com.example.interview.room.domain.InterviewRoomStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JoinInterviewRoomResponse {

    private Long roomId;
    private String title;
    InterviewRoomStatus status;
    ParticipantRole role;

    // Role 정보가 없을 때 사용하는 생성자
    public JoinInterviewRoomResponse(Long roomId, String title, InterviewRoomStatus status) {
        this.roomId = roomId;
        this.title = title;
        this.status = status;
        this.role = null;
    }

    // Role 정보가 존재할 때 사용하는 생성자
    public JoinInterviewRoomResponse(Long roomId, String title, InterviewRoomStatus status, ParticipantRole role) {
        this.roomId = roomId;
        this.title = title;
        this.status = status;
        this.role = role;
    }
}
