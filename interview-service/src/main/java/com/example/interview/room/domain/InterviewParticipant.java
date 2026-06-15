package com.example.interview.room.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_interview_participant_room_user",
                        columnNames = {"room_id", "user_id"}
                )
        }
)
@Builder
public class InterviewParticipant {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long roomId;

    private Long userId; // 참여자 id

    @Enumerated(EnumType.STRING)
    private ParticipantRole role; // 면접방 내 역할

    @Enumerated(EnumType.STRING)
    private ParticipantStatus status; // 참여 상태

    private LocalDateTime joinedAt; // 최초 입장 시간
    private LocalDateTime leftAt; // 퇴장 시간


    public static InterviewParticipant create(Long roomId, Long userId, ParticipantRole role) {
        return InterviewParticipant.builder()
                .roomId(roomId)
                .userId(userId)
                .status(ParticipantStatus.JOINED)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    public void updateStatus(ParticipantStatus newStatus) {
        this.status = newStatus;
    }


    public void leave() {
        this.status = ParticipantStatus.LEFT;
        this.leftAt = LocalDateTime.now();
    }

}
