package com.owlexa.owlexabackend.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit test cho GlobalExceptionHandler — đứng độc lập với Spring context.
 *
 * Dùng MockMvcBuilders.standaloneSetup thay vì @SpringBootTest:
 *   - Không cần load DB, JWT filter, security → chạy < 100ms
 *   - Chỉ test mapping giữa exception → HTTP response
 *   - Controller test nội bộ throw exception, handler bắt và convert sang JSON
 *
 * Phạm vi test: GlobalExceptionHandler có handle đúng 2 exception mới
 * (BusinessRuleException, TenancyViolationException) và KHÔNG vô tình
 * nuốt BadRequestException cũ.
 */
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
    @DisplayName("BusinessRuleException → HTTP 422 với message gốc")
    void businessRuleException_shouldReturn422WithMessage() throws Exception {
        mockMvc.perform(get("/test/business-rule"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("Học phí đã đóng đủ"));
    }

    @Test
    @DisplayName("TenancyViolationException → HTTP 403 với message ẩn danh tiếng Việt")
    void tenancyViolationException_shouldReturn403WithAnonymousMessage() throws Exception {
        mockMvc.perform(get("/test/tenancy-violation"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Không tìm thấy dữ liệu"));
    }

    @Test
    @DisplayName("BadRequestException (cũ) vẫn hoạt động — backward-compatible")
    void badRequestException_shouldStillReturn400() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Thiếu field bắt buộc"));
    }

    @RestController
    static class TestExceptionController {

        @GetMapping("/test/business-rule")
        public void businessRule() {
            throw new BusinessRuleException("Học phí đã đóng đủ");
        }

        @GetMapping("/test/tenancy-violation")
        public void tenancyViolation() {
            throw new TenancyViolationException("User 5 cố truy cập fee_record 999 của center 8");
        }

        @GetMapping("/test/bad-request")
        public void badRequest() {
            throw new BadRequestException("Thiếu field bắt buộc");
        }
    }
}
