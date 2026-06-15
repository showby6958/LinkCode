package com.example.interview.interviewDocument.repository;

import com.example.interview.interviewDocument.domain.InterviewDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterviewDocumentRepository extends JpaRepository<InterviewDocument, Long> {

    Optional<InterviewDocument> findByInterviewRoomId(Long roomId);
}
