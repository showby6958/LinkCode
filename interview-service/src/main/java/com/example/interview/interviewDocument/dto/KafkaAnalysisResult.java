package com.example.interview.interviewDocument.dto;

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class KafkaAnalysisResult {

    private Long roomId;
    private String documentId;

    // [Producer 전송용]자바가 파이썬 AI 분석 서비스로 (AI 분석 요청) 넘겨줄 원본 소스 코드
    private String codeContent; // Redis에서 수거한 최종 평문 코드

    private String problemContent;

    // [Consumer 수신용]파이썬 AI가 채워서 다시 자바 서비스로 돌려줄 피드백 결과
    private int pythonicScore; // AI 가 책정한 코드 점수
    private String codeCritique; // AI의 코드 상세 리뷰 및 개선 방향 피드백
    private String timeComplexity; // AI가 계산한 예상 시간 복잡도
    private String edgeCase;
    private String followUpQuestions;
}
