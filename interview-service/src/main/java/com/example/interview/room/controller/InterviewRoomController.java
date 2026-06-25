package com.example.interview.room.controller;

import com.example.common.security.CustomPrincipal;
import com.example.interview.room.dto.*;
import com.example.interview.room.service.InterviewRoomService;

import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/interview/room")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class InterviewRoomController {

    private final InterviewRoomService interviewRoomService;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')") // ROLE_ADMIN 권한을 가진 사용자만 이 API 호출 가능
    public ResponseEntity<CreateInterviewRoomResponse> createInterviewRoom(
            @Valid @RequestBody CreateInterviewRoomRequest request,
            @AuthenticationPrincipal CustomPrincipal user
    ) {
       CreateInterviewRoomResponse response = interviewRoomService.create(request, user.getUserId());

       return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/join/{inviteCode}")
    public ResponseEntity<JoinInterviewRoomResponse> joinInterviewRoom(
            @PathVariable String inviteCode,
            @AuthenticationPrincipal CustomPrincipal user
    ) {
        JoinInterviewRoomResponse response = interviewRoomService.join(
                inviteCode,
                user.getUserId()
        );

        return ResponseEntity.ok(response);
    }


    @GetMapping("/info/{roomId}")
    public ResponseEntity<InterviewRoomInfo> getRoomInfo(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomPrincipal user
    ) {
        InterviewRoomInfo info = interviewRoomService.getRoomInfo(roomId, user.getUserId());

        return ResponseEntity.ok(info);
    }

    @GetMapping("/my")
    public ResponseEntity<List<MyRoomResponse>> getMyRooms(
            @AuthenticationPrincipal CustomPrincipal user
    ) {
        List<MyRoomResponse> rooms = interviewRoomService.getMyRooms(user.getUserId());
        return ResponseEntity.ok(rooms);
    }

}
