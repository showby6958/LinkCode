package com.example.interview.room.dto;

import com.example.interview.room.domain.InterviewRoomStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRoomStatusResponse {

    private InterviewRoomStatus status;
}
