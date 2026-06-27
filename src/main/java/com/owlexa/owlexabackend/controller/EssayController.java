package com.owlexa.owlexabackend.controller;

import com.owlexa.owlexabackend.dto.request.EssayRubricRequest;
import com.owlexa.owlexabackend.dto.request.EssaySubmitRequest;
import com.owlexa.owlexabackend.dto.request.ManualFeedbackRequest;
import com.owlexa.owlexabackend.dto.response.EssayDetailResponse;
import com.owlexa.owlexabackend.dto.response.EssayGradingResultResponse;
import com.owlexa.owlexabackend.dto.response.EssayRubricResponse;
import com.owlexa.owlexabackend.dto.response.EssaySubmissionResponse;
import com.owlexa.owlexabackend.service.EssayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EssayController {

    private final EssayService essayService;

    @GetMapping("/teacher/essay-rubrics/me")
    public List<EssayRubricResponse> findMyRubricsAsTeacher() {
        return essayService.findMyRubricsAsTeacher();
    }

    @PostMapping("/teacher/essay-rubrics")
    public EssayRubricResponse createRubric(@RequestBody EssayRubricRequest request) {
        return essayService.createRubric(request);
    }

    @PostMapping("/essays/submit")
    public EssaySubmissionResponse submitEssay(@RequestBody EssaySubmitRequest request) {
        return essayService.submitEssay(request);
    }

    @GetMapping("/student/essays/me")
    public List<EssaySubmissionResponse> findMyEssays() {
        return essayService.findMyEssays();
    }

    @GetMapping("/essays/{essayId}")
    public EssayDetailResponse getEssay(@PathVariable Long essayId) {
        return essayService.getEssay(essayId);
    }

    @GetMapping("/essays/{essayId}/grading-result")
    public EssayGradingResultResponse getGradingResult(@PathVariable Long essayId) {
        return essayService.getGradingResult(essayId);
    }

    @GetMapping("/teacher/classes/{classId}/essays")
    public List<EssaySubmissionResponse> findClassEssays(@PathVariable Long classId) {
        return essayService.findClassEssays(classId);
    }

    @PostMapping("/essays/{essayId}/manual-feedback")
    public void addManualFeedback(@PathVariable Long essayId, @RequestBody ManualFeedbackRequest request) {
        essayService.addManualFeedback(essayId, request.getFeedback());
    }
}
