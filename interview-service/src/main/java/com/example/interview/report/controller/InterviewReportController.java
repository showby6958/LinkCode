package com.example.interview.report.controller;

import com.example.interview.report.dto.InterviewReportResponse;
import com.example.interview.report.service.InterviewReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interview/reports")
@RequiredArgsConstructor
public class InterviewReportController {

    private final InterviewReportService reportService;

    @GetMapping("/{documentId}")
    public ResponseEntity<InterviewReportResponse> getReport(
            @PathVariable Long documentId
    ) {
        InterviewReportResponse response = reportService.getReportByDocumentId(documentId);

        return ResponseEntity.ok(response);
    }
}
