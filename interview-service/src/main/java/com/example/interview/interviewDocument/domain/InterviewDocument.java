package com.example.interview.interviewDocument.domain;

import com.example.interview.room.domain.InterviewRoom;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InterviewDocument {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_room_id", nullable = false)
    private InterviewRoom interviewRoom;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    private String language;

    private String fileName; // 파일명

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content; // 텍스트 내용 (실시간 코드 저장)

    private String fileUrl; // S3 등에 업로드된 실제 파일 경로 (나중에 파일 업로드 시 활용)

    // ** AI 분석 결과 필드 **
    private Integer pythonicScore; // AI가 채점한 점수

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String codeCritique; // 코드 상세 리뷰 및 피드백

    private String edgeCase;

    private String followUpQuestions;

    private String timeComplexity; // 시간 복잡도 분석 결과

    private LocalDateTime createdAt;
    private LocalDateTime analyzedAt; // 분석완료 시점

    // AI 결과를 반영하는 더티 체킹용 메서드
    public void updateAiFeedback(Integer pythonicScore, String codeCritique, String edgeCase, String followUpQuestions, String timeComplexity) {
        this.pythonicScore = pythonicScore;
        this.codeCritique = codeCritique;
        this.edgeCase = edgeCase;
        this.followUpQuestions = followUpQuestions;
        this.timeComplexity = timeComplexity;
        this.analyzedAt = LocalDateTime.now();
    }

    public static InterviewDocument createCodeDocument(InterviewRoom room, String code) {
        return InterviewDocument.builder()
                .interviewRoom(room)
                .documentType(DocumentType.SOURCE_CODE)
                .fileName("test.py")
                .content(code)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
