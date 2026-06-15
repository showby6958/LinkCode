package com.example.interview.room.dto;

import com.example.interview.room.domain.InterviewRoomStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRoomInfo {

    private Long roomId;
    private Long documentId;
    private InterviewRoomStatus status;
    private String problemContent;

}
