package com.owlexa.owlexabackend.modules.mocktest.controller;
import com.owlexa.owlexabackend.modules.mocktest.dto.request.MockTestQuestionRequest;
import com.owlexa.owlexabackend.modules.mocktest.dto.request.MockTestRequest;
import com.owlexa.owlexabackend.modules.mocktest.dto.request.MockTestSaveAnswerRequest;
import com.owlexa.owlexabackend.modules.mocktest.dto.request.MockTestSubmitRequest;
import com.owlexa.owlexabackend.modules.mocktest.dto.response.MockTestAttemptResponse;
import com.owlexa.owlexabackend.modules.mocktest.dto.response.MockTestQuestionResponse;
import com.owlexa.owlexabackend.modules.mocktest.dto.response.MockTestResponse;
import com.owlexa.owlexabackend.modules.mocktest.service.MockTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MockTestController {

    private final MockTestService mockTestService;

    @GetMapping("/owner/mock-tests")
    @PreAuthorize("hasAuthority('TEST_VIEW')")
    public List<MockTestResponse> getOwnerTests() {
        return mockTestService.getOwnerTests();
    }

    @PostMapping("/owner/mock-tests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TEST_CREATE')")
    public MockTestResponse createTest(@Valid @RequestBody MockTestRequest request) {
        return mockTestService.createTest(request);
    }

    @PutMapping("/owner/mock-tests/{testId}")
    @PreAuthorize("hasAuthority('TEST_CREATE')")
    public MockTestResponse updateTest(@PathVariable Long testId, @Valid @RequestBody MockTestRequest request) {
        return mockTestService.updateTest(testId, request);
    }

    @DeleteMapping("/owner/mock-tests/{testId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TEST_CREATE')")
    public void deleteTest(@PathVariable Long testId) {
        mockTestService.deleteTest(testId);
    }

    @GetMapping("/owner/mock-tests/{testId}/questions")
    public List<MockTestQuestionResponse> getOwnerQuestions(@PathVariable Long testId) {
        return mockTestService.getQuestionsForOwner(testId);
    }

    @PostMapping("/owner/mock-tests/{testId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public MockTestQuestionResponse addQuestion(@PathVariable Long testId, @Valid @RequestBody MockTestQuestionRequest request) {
        return mockTestService.addQuestion(testId, request);
    }

    @PutMapping("/owner/mock-tests/{testId}/questions/{questionId}")
    public MockTestQuestionResponse updateQuestion(
            @PathVariable Long testId,
            @PathVariable Long questionId,
            @Valid @RequestBody MockTestQuestionRequest request
    ) {
        return mockTestService.updateQuestion(testId, questionId, request);
    }

    @DeleteMapping("/owner/mock-tests/{testId}/questions/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(@PathVariable Long testId, @PathVariable Long questionId) {
        mockTestService.deleteQuestion(testId, questionId);
    }

    @GetMapping("/owner/mock-tests/{testId}/attempts")
    public List<MockTestAttemptResponse> getOwnerAttempts(@PathVariable Long testId) {
        return mockTestService.getAttemptsForOwner(testId);
    }

    @GetMapping("/owner/mock-tests/attempts/{attemptId}")
    public MockTestAttemptResponse getOwnerAttempt(@PathVariable Long attemptId) {
        return mockTestService.getAttemptForOwner(attemptId);
    }

    @GetMapping("/teacher/mock-tests/attempts")
    public List<MockTestAttemptResponse> getTeacherAttempts(@RequestParam(required = false) Long classId) {
        return mockTestService.getAttemptsForTeacher(classId);
    }

    @GetMapping("/teacher/mock-tests/attempts/{attemptId}")
    public MockTestAttemptResponse getTeacherAttempt(@PathVariable Long attemptId) {
        return mockTestService.getAttemptForTeacher(attemptId);
    }

    @GetMapping("/student/mock-tests")
    public List<MockTestResponse> getStudentTests() {
        return mockTestService.getAvailableTestsForStudent();
    }

    @GetMapping("/student/mock-tests/{testId}/questions")
    public List<MockTestQuestionResponse> getStudentQuestions(@PathVariable Long testId) {
        return mockTestService.getQuestionsForStudent(testId);
    }

    @PostMapping("/mock-tests/{testId}/start")
    public MockTestAttemptResponse startTest(@PathVariable Long testId) {
        return mockTestService.startTest(testId);
    }

    @PostMapping("/mock-tests/{testId}/answers/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveAnswer(
            @PathVariable Long testId,
            @PathVariable Long questionId,
            @Valid @RequestBody MockTestSaveAnswerRequest request
    ) {
        mockTestService.saveAnswer(testId, questionId, request);
    }

    @PostMapping("/mock-tests/{testId}/submit")
    public MockTestAttemptResponse submitTest(@PathVariable Long testId, @Valid @RequestBody MockTestSubmitRequest request) {
        return mockTestService.submitTest(testId, request);
    }

    @GetMapping("/student/mock-tests/results")
    public List<MockTestAttemptResponse> getMyResults() {
        return mockTestService.getMyResults();
    }

    @GetMapping("/mock-tests/results/{attemptId}")
    public MockTestAttemptResponse getResult(@PathVariable Long attemptId) {
        return mockTestService.getMyResult(attemptId);
    }

    @GetMapping("/mock-tests/attempts/{attemptId}")
    public MockTestAttemptResponse getAttempt(@PathVariable Long attemptId) {
        return mockTestService.getAttemptForStudent(attemptId);
    }
}
