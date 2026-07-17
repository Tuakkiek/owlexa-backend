package com.owlexa.owlexabackend.modules.essay.controller;
import com.owlexa.owlexabackend.modules.essay.dto.request.EssayRubricRequest;
import com.owlexa.owlexabackend.modules.essay.dto.request.EssaySubmitRequest;
import com.owlexa.owlexabackend.modules.essay.dto.request.ManualFeedbackRequest;
import com.owlexa.owlexabackend.modules.essay.dto.response.EssayDetailResponse;
import com.owlexa.owlexabackend.modules.essay.dto.response.EssayGradingResultResponse;
import com.owlexa.owlexabackend.modules.essay.dto.response.EssayRubricResponse;
import com.owlexa.owlexabackend.modules.essay.dto.response.EssaySubmissionResponse;
import com.owlexa.owlexabackend.modules.essay.service.EssayService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('ESSAY_VIEW')")
    public List<EssayRubricResponse> findMyRubricsAsTeacher() {
        return essayService.findMyRubricsAsTeacher();
    }

    @PostMapping("/teacher/essay-rubrics")
    @PreAuthorize("hasAuthority('ESSAY_VIEW')")
    public EssayRubricResponse createRubric(@RequestBody EssayRubricRequest request) {
        return essayService.createRubric(request);
    }

    @PostMapping("/essays/submit")
    @PreAuthorize("hasAuthority('ESSAY_SUBMIT')")
    public EssaySubmissionResponse submitEssay(@RequestBody EssaySubmitRequest request) {
        return essayService.submitEssay(request);
    }

    @GetMapping("/student/essays/me")
    @PreAuthorize("hasAnyAuthority('ESSAY_VIEW', 'ESSAY_SUBMIT')")
    public List<EssaySubmissionResponse> findMyEssays() {
        return essayService.findMyEssays();
    }

    @GetMapping("/essays/{essayId}")
    @PreAuthorize("hasAnyAuthority('ESSAY_VIEW', 'ESSAY_SUBMIT', 'ESSAY_GRADE')")
    public EssayDetailResponse getEssay(@PathVariable Long essayId) {
        return essayService.getEssay(essayId);
    }

    @GetMapping("/essays/{essayId}/grading-result")
    @PreAuthorize("hasAnyAuthority('ESSAY_VIEW', 'ESSAY_SUBMIT', 'ESSAY_GRADE')")
    public EssayGradingResultResponse getGradingResult(@PathVariable Long essayId) {
        return essayService.getGradingResult(essayId);
    }

    @GetMapping("/teacher/classes/{classId}/essays")
    @PreAuthorize("hasAuthority('ESSAY_GRADE')")
    public List<EssaySubmissionResponse> findClassEssays(@PathVariable Long classId) {
        return essayService.findClassEssays(classId);
    }

    @PostMapping("/essays/{essayId}/manual-feedback")
    @PreAuthorize("hasAuthority('ESSAY_GRADE')")
    public void addManualFeedback(@PathVariable Long essayId, @RequestBody ManualFeedbackRequest request) {
        essayService.addManualFeedback(essayId, request.getFeedback());
    }
}
