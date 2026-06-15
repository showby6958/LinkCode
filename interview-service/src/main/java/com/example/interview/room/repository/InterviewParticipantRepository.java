package com.example.interview.room.repository;

import com.example.interview.room.domain.InterviewParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewParticipantRepository extends JpaRepository<InterviewParticipant, Long> {

    Optional<InterviewParticipant> findByRoomIdAndUserId(Long roomId, Long userId);

    List<InterviewParticipant> findAllByRoomId(Long roomId);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);
}
