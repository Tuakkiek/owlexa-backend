package com.owlexa.owlexabackend.common.exception;

import com.owlexa.owlexabackend.modules.assessment_builder.exception.AssessmentDocumentIntegrityException;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestExceptionController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("BusinessRuleException returns HTTP 422 with original message")
    void businessRuleException_shouldReturn422WithMessage() throws Exception {
        mockMvc.perform(get("/test/business-rule"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("Tuition is already paid"));
    }

    @Test
    @DisplayName("TenancyViolationException returns HTTP 403 with anonymized message")
    void tenancyViolationException_shouldReturn403WithAnonymousMessage() throws Exception {
        mockMvc.perform(get("/test/tenancy-violation"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Không tìm thấy dữ liệu"));
    }

    @Test
    @DisplayName("BadRequestException remains backward-compatible")
    void badRequestException_shouldStillReturn400() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Missing required field"));
    }

    @Test
    @DisplayName("ConflictException returns HTTP 409 with stable code and message")
    void conflictException_shouldReturn409WithCodeAndMessage() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSESSMENT_VERSION_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Assessment has been modified; refresh before saving"));
    }

    @Test
    @DisplayName("Spring optimistic lock exception returns HTTP 409 without persistence details")
    void springOptimisticLockException_shouldReturn409WithSanitizedBody() throws Exception {
        mockMvc.perform(get("/test/spring-optimistic-lock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPTIMISTIC_LOCK_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Dữ liệu vừa được cập nhật bởi thao tác khác. Vui lòng tải lại và thử lại."));
    }

    @Test
    @DisplayName("JPA optimistic lock exception returns HTTP 409 without persistence details")
    void jpaOptimisticLockException_shouldReturn409WithSanitizedBody() throws Exception {
        mockMvc.perform(get("/test/jpa-optimistic-lock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPTIMISTIC_LOCK_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Dữ liệu vừa được cập nhật bởi thao tác khác. Vui lòng tải lại và thử lại."));
    }

    @Test
    @DisplayName("AssessmentDocumentIntegrityException returns HTTP 500 with sanitized body")
    void assessmentDocumentIntegrityException_shouldReturn500WithSanitizedBody() throws Exception {
        mockMvc.perform(get("/test/assessment-document-integrity"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ASSESSMENT_DOCUMENT_INTEGRITY_ERROR"))
                .andExpect(jsonPath("$.message").value("Assessment document data is inconsistent"))
                .andExpect(content().string(not(containsString("42"))))
                .andExpect(content().string(not(containsString("77"))))
                .andExpect(content().string(not(containsString("missing assessmentItem"))));
    }

    @Test
    @DisplayName("DataIntegrityViolationException returns a friendly message for duplicate teacher review creation")
    void dataIntegrityViolationException_shouldReturnFriendlyDuplicateReviewMessage() throws Exception {
        mockMvc.perform(get("/test/data-integrity-review-duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Phiếu chấm cho lượt nộp này đã tồn tại. Vui lòng tải lại trang."));
    }

    @Test
    @DisplayName("Audio endpoint throwing exception returns JSON response with application/json content type")
    void audioEndpointException_shouldReturnJsonContentType() throws Exception {
        mockMvc.perform(get("/test/audio-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Content-Type", containsString("application/json")))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Audio file not found"));
    }

    @RestController
    static class TestExceptionController {

        @GetMapping(value = "/test/audio-not-found", produces = "audio/mpeg")
        public void audioNotFound() {
            throw new ResourceNotFoundException("Audio file not found");
        }

        @GetMapping("/test/business-rule")
        public void businessRule() {
            throw new BusinessRuleException("Tuition is already paid");
        }

        @GetMapping("/test/tenancy-violation")
        public void tenancyViolation() {
            throw new TenancyViolationException("User 5 tried to access fee_record 999 from center 8");
        }

        @GetMapping("/test/bad-request")
        public void badRequest() {
            throw new BadRequestException("Missing required field");
        }

        @GetMapping("/test/conflict")
        public void conflict() {
            throw new ConflictException(
                    "ASSESSMENT_VERSION_CONFLICT",
                    "Assessment has been modified; refresh before saving"
            );
        }

        @GetMapping("/test/spring-optimistic-lock")
        public void springOptimisticLock() {
            throw new ObjectOptimisticLockingFailureException("Assessment", 7L);
        }

        @GetMapping("/test/jpa-optimistic-lock")
        public void jpaOptimisticLock() {
            throw new OptimisticLockException("leaky entity detail");
        }

        @GetMapping("/test/assessment-document-integrity")
        public void assessmentDocumentIntegrity() {
            throw new AssessmentDocumentIntegrityException("Published question block 42 is missing assessmentItem 77");
        }

        @GetMapping("/test/data-integrity-review-duplicate")
        public void dataIntegrityReviewDuplicate() {
            throw new DataIntegrityViolationException(
                    "Duplicate entry '11' for key 'teacher_reviews.uk_teacher_reviews_submission_attempt_id'"
            );
        }
    }
}
