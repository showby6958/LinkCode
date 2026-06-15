package com.example.interview.report.dto;

import com.example.interview.interviewDocument.domain.InterviewDocument;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewReportResponse {

    private Long roomId;
    private String roomTitle;
    private Long documentId;
    private String fileName;
    private String finalCode; // 면접자 최종 소스코드
    private Integer pythonicScore; // AI 점수
    private String codeCritique; // AI 코드 리뷰
    private String edgeCase;
    private String followUpQuestions;
    private String timeComplexity; // 시간 복잡도
    private LocalDateTime analyzedAt; // 분석 완료 시간

    public static InterviewReportResponse fromEntity(InterviewDocument document) {
        return InterviewReportResponse.builder()
                .roomId(document.getInterviewRoom().getId())
                .roomTitle(document.getInterviewRoom().getTitle())
                .documentId(document.getId())
                .fileName(document.getFileName())
                .finalCode(document.getContent())
                .pythonicScore(document.getPythonicScore())
                .codeCritique(document.getCodeCritique())
                .edgeCase(document.getEdgeCase())
                .followUpQuestions(document.getFollowUpQuestions())
                .timeComplexity(document.getTimeComplexity())
                .analyzedAt(document.getAnalyzedAt())
                .build();
    }
}