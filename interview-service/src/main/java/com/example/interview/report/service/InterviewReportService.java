package com.example.interview.report.service;

import com.example.interview.interviewDocument.domain.InterviewDocument;
import com.example.interview.interviewDocument.repository.InterviewDocumentRepository;
import com.example.interview.report.dto.InterviewReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewReportService {

    private final InterviewDocumentRepository documentRepository;

    @Transactional(readOnly = true)
    public InterviewReportResponse getReportByDocumentId(Long documentId) {

        InterviewDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 AI 분석 리포트가 존재하지 않습니다."));

        return InterviewReportResponse.fromEntity(document);
    }

}
