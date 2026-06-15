package com.example.interview.message.service;

import com.example.interview.message.domain.InterviewMessage;
import com.example.interview.message.dto.InterviewMessageRequest;
import com.example.interview.message.dto.InterviewMessageResponse;
import com.example.interview.message.repository.InterviewMessageRepository;
import com.example.interview.room.domain.InterviewRoom;
import com.example.interview.room.repository.InterviewParticipantRepository;
import com.example.interview.room.repository.InterviewRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewMessageService {

    private final InterviewRoomRepository interviewRoomRepository;
    private final InterviewParticipantRepository participantRepository;
    private final InterviewMessageRepository messageRepository;

    @Transactional
    public InterviewMessageResponse sendMessage(Long roomId, Long senderId, String senderName, String senderPicture, InterviewMessageRequest request) {

        InterviewRoom room = interviewRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        validateParticipant(roomId, senderId);
        validateChatAvailable(room);

        InterviewMessage message = InterviewMessage.chat(
                roomId,
                senderId,
                senderName,
                senderPicture,
                request.getContent()
        );

        InterviewMessage savedMessage = messageRepository.save(message);

        return InterviewMessageResponse.from(savedMessage);
    }


    @Transactional(readOnly = true)
    public List<InterviewMessageResponse> getMessageHistory(Long roomId, Long userId) {
        // 1. 존재하는 방인지 체크
        InterviewRoom room = interviewRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        // 2. 해당 방의 참여자인지 체크
        validateParticipant(roomId, userId);

        // 3. 메시지 히스토리 조회 후 DTO 변환
        return messageRepository.findAllByRoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(InterviewMessageResponse::from)
                .collect(Collectors.toList());
    }

    private void validateParticipant(Long roomId, Long userId) {
        boolean exists = participantRepository.existsByRoomIdAndUserId(roomId, userId);

        if (!exists) {
            throw new IllegalArgumentException("면접방 참가자만 메시지를 보낼 수 있습니다.");
        }
    }

    private void validateChatAvailable(InterviewRoom room) {
        if (room.isEnded()) {
            throw new IllegalStateException("현재 면접방 상태에서는 채팅할 수 없습니다. status: " + room.getStatus());
        }
    }

}
