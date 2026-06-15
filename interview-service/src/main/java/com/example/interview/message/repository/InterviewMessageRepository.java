package com.example.interview.message.repository;

import com.example.interview.message.domain.InterviewMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewMessageRepository extends JpaRepository<InterviewMessage, Long> {

    List<InterviewMessage> findAllByRoomIdOrderByCreatedAtAsc(Long roomId);
}
