package com.example.interview.room.service;

import com.example.interview.interviewDocument.domain.InterviewDocument;
import com.example.interview.interviewDocument.repository.InterviewDocumentRepository;
import com.example.interview.room.domain.*;
import com.example.interview.room.dto.InterviewRoomInfo;
import com.example.interview.room.dto.JoinInterviewRoomResponse;
import com.example.interview.room.dto.MyRoomResponse;
import com.example.interview.room.repository.InterviewParticipantRepository;
import com.example.interview.room.dto.CreateInterviewRoomRequest;
import com.example.interview.room.dto.CreateInterviewRoomResponse;
import com.example.interview.room.repository.InterviewRoomRepository;
import com.example.interview.room.util.InviteCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewRoomService {

    private final InterviewRoomRepository interviewRoomRepository;
    private final InterviewParticipantRepository participantRepository;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final InterviewDocumentRepository documentRepository;


    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    @Transactional
    public CreateInterviewRoomResponse create(CreateInterviewRoomRequest request, Long userId) {

        String inviteCode = inviteCodeGenerator.generate();

        InterviewRoom room = InterviewRoom.create(
                request.getTitle(),
                request.getProblemContent(),
                userId,
                inviteCode,
                LocalDateTime.now()
        );

        InterviewRoom savedRoom = interviewRoomRepository.save(room);

        InterviewParticipant interviewer = InterviewParticipant.create(
                savedRoom.getId(),
                userId,
                ParticipantRole.INTERVIEWER
        );

        participantRepository.save(interviewer);

        return new CreateInterviewRoomResponse(
                savedRoom.getId(),
                savedRoom.getTitle(),
                savedRoom.getStatus(),
                savedRoom.getInviteCode(),
                buildInviteUrl(savedRoom.getInviteCode())
        );
    }

    private String buildInviteUrl(String inviteCode) {
        return frontendBaseUrl + "/interviews/join/" + inviteCode;
    }


    @Transactional
    public JoinInterviewRoomResponse join(String inviteCode, Long userId) {
        InterviewRoom room = interviewRoomRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대 코드입니다."));


        Optional<InterviewParticipant> existingParticipant = participantRepository.findByRoomIdAndUserId(room.getId(), userId);

        InterviewParticipant participant;

        if (existingParticipant.isPresent()) {
            // 이미 참여한 이력이 있다면 기존 객체를 그대로 재활용
            participant = existingParticipant.get();
            log.info("[Re-join]유저: {}`가 면접방: {}에 재입장 합니다.", userId, room.getId());

            if (ParticipantStatus.DISCONNECTED.equals(participant.getStatus())) {
                participant.updateStatus(ParticipantStatus.JOINED);
                log.info("[Status-update] 유저 {} 의 상태를 {}로 재활성화했습니다.", userId, ParticipantStatus.JOINED);
            }
        } else {
            // 방 개설자와 현재 유저 id를 비교해서 Role 결정
            ParticipantRole assignedRole = room.isCreater(userId) ? ParticipantRole.INTERVIEWER : ParticipantRole.CANDIDATE;

            participant = InterviewParticipant.create(
                    room.getId(),
                    userId,
                    assignedRole
            );
            participantRepository.save(participant);

            // (더티체킹)참여자 role이 CANDIDATE 일 경우만 방 상태 READY로 변경
            if (ParticipantRole.CANDIDATE.equals(assignedRole)) {
                room.start(); // 엔티티 내부 필드가 바뀜 -> 트랜잭션 종료 시 DB에 자동 Update 쿼리 반영
            }
        }

        return new JoinInterviewRoomResponse(
                room.getId(),
                room.getTitle(),
                room.getStatus(),
                participant.getRole()
        );
    }



    @Transactional(readOnly = true)
    public InterviewRoomInfo getRoomInfo(Long roomId, Long userId) {

        // 방 멤버십 검증(비즈니스 규칙). 예전엔 스프링 시큐리티의 AccessDeniedException을
        // 던져 403으로 변환됐는데, 시큐리티 의존을 제거하면서 표준 ResponseStatusException으로
        // 바꿨다. 게이트웨이의 역할 검사와는 다른, 이 방에 속했는지를 보는 검사다.
        InterviewParticipant participant = participantRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "이 면접방의 멤버가 아닙니다. 초대코드를 통해 입장하세요."));

        InterviewRoom room = interviewRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 파기된 방입니다. ID: " + roomId));

        Long documentId = documentRepository.findByInterviewRoomId(roomId)
                .map(InterviewDocument::getId)
                .orElse(null);

        return new InterviewRoomInfo(room.getId(), documentId, room.getStatus(), room.getProblemContent());
    }

    @Transactional(readOnly = true)
    public List<MyRoomResponse> getMyRooms(Long userId) {
        List<InterviewParticipant> participants = participantRepository.findAllByUserId(userId);

        if (participants.isEmpty()) {
            return List.of();
        }

        List<Long> roomIds = participants.stream()
                .map(InterviewParticipant::getRoomId)
                .toList();

        Map<Long, ParticipantRole> roleByRoomId = participants.stream()
                .collect(Collectors.toMap(InterviewParticipant::getRoomId, InterviewParticipant::getRole));

        return interviewRoomRepository.findAllById(roomIds).stream()
                .map(room -> new MyRoomResponse(
                        room.getId(),
                        room.getTitle(),
                        room.getStatus(),
                        roleByRoomId.get(room.getId()),
                        room.getCreatedAt()
                ))
                .sorted(Comparator.comparing(MyRoomResponse::getCreatedAt).reversed())
                .toList();
    }

}
