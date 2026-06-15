package com.example.interview.room.repository;

import com.example.interview.room.domain.InterviewRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterviewRoomRepository extends JpaRepository<InterviewRoom, Long> {

    boolean existsByInviteCode(String inviteCode);

    Optional<InterviewRoom> findByInviteCode(String inviteCode);
}
