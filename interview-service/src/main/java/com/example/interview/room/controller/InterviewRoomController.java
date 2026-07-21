package com.example.interview.room.controller;

import com.example.interview.room.dto.*;
import com.example.interview.room.service.InterviewRoomService;
import com.example.interview.security.CurrentUser;
import com.example.interview.security.LoginUser;

import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/interview/room")
@RequiredArgsConstructor
public class InterviewRoomController {

    private final InterviewRoomService interviewRoomService;

    // 방 생성은 ADMIN만 — 역할 검사는 게이트웨이(RoleRule)가 담당한다.
    // CORS도 게이트웨이가 처리하므로 @CrossOrigin을 두지 않는다.
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CreateInterviewRoomResponse> createInterviewRoom(
            @Valid @RequestBody CreateInterviewRoomRequest request,
            @CurrentUser LoginUser user
    ) {
       CreateInterviewRoomResponse response = interviewRoomService.create(request, user.userId());

       return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/join/{inviteCode}")
    public ResponseEntity<JoinInterviewRoomResponse> joinInterviewRoom(
            @PathVariable String inviteCode,
            @CurrentUser LoginUser user
    ) {
        JoinInterviewRoomResponse response = interviewRoomService.join(
                inviteCode,
                user.userId()
        );

        return ResponseEntity.ok(response);
    }


    @GetMapping("/info/{roomId}")
    public ResponseEntity<InterviewRoomInfo> getRoomInfo(
            @PathVariable Long roomId,
            @CurrentUser LoginUser user
    ) {
        InterviewRoomInfo info = interviewRoomService.getRoomInfo(roomId, user.userId());

        return ResponseEntity.ok(info);
    }

    @GetMapping("/my")
    public ResponseEntity<List<MyRoomResponse>> getMyRooms(
            @CurrentUser LoginUser user
    ) {
        List<MyRoomResponse> rooms = interviewRoomService.getMyRooms(user.userId());
        return ResponseEntity.ok(rooms);
    }

}
