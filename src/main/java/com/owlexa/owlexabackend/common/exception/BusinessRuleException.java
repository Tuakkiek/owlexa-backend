package com.owlexa.owlexabackend.common.exception;

/**
 * Nghiệp vụ bị vi phạm — HTTP 422 Unprocessable Entity.
 *
 * Khác BadRequestException (400, request sai format):
 *   - 400: client gửi request malformed (thiếu field, JSON sai cú pháp).
 *   - 422: client gửi request HỢP LỆ, nhưng vi phạm business rule
 *          (vd: thu tiền vượt học phí, đăng ký trùng lịch, nộp bài sau deadline).
 *
 * Frontend dựa vào status code để:
 *   - 400 → show form validation
 *   - 422 → show dialog "không thể thực hiện vì [message]"
 */
public class BusinessRuleException extends RuntimeException {
    private String code;

    public BusinessRuleException(String message) {
        super(message);
    }

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
