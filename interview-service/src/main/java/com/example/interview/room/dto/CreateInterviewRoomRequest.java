package com.example.interview.room.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CreateInterviewRoomRequest {

    @Size(max = 50)
    private String title;
    String problemContent;

    private LocalDateTime createdAt;
}
