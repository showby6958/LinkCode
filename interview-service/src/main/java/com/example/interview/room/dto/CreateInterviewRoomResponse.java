package com.example.interview.room.dto;

import com.example.interview.room.domain.InterviewRoomStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateInterviewRoomResponse {

    private Long roomId;
    private String title;
    private InterviewRoomStatus status;
    private String inviteCode;
    private String inviteUrl;
}
