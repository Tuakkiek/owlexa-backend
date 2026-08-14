package com.owlexa.owlexabackend.common.exception;

import com.owlexa.owlexabackend.modules.assessment_builder.exception.AssessmentDocumentIntegrityException;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(errorBody(
                400,
                "Yêu cầu định dạng JSON không hợp lệ",
                null
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        String firstMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(msg -> msg != null && !msg.isBlank())
                .findFirst()
                .orElse("Dữ liệu gửi lên không hợp lệ");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(errorBody(
                400,
                firstMessage,
                errors
        ));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateResourceException ex) {
        log.error("[409] DuplicateResourceException thrown: class={}, message={}", ex.getClass().getName(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(errorBody(
                409,
                ex.getMessage(),
                null
        ));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "OPTIMISTIC_LOCK_CONFLICT");
        body.put("message", "Dữ liệu vừa được cập nhật bởi thao tác khác. Vui lòng tải lại và thử lại.");
        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<Map<String, Object>> handleJpaOptimisticLock(OptimisticLockException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "OPTIMISTIC_LOCK_CONFLICT");
        body.put("message", "Dữ liệu vừa được cập nhật bởi thao tác khác. Vui lòng tải lại và thử lại.");
        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    @ExceptionHandler(AssessmentDocumentIntegrityException.class)
    public ResponseEntity<Map<String, Object>> handleAssessmentDocumentIntegrity(
            AssessmentDocumentIntegrityException ex
    ) {
        log.error("[500] AssessmentDocumentIntegrityException thrown: {}", ex.getMessage(), ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "ASSESSMENT_DOCUMENT_INTEGRITY_ERROR");
        body.put("message", "Assessment document data is inconsistent");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body(errorBody(
                404,
                ex.getMessage(),
                null
        ));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
        log.error("[400] BadRequestException thrown: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(errorBody(
                400,
                ex.getMessage(),
                null
        ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).contentType(MediaType.APPLICATION_JSON).body(errorBody(
                413,
                "Dung lượng tệp vượt quá giới hạn cho phép (tối đa 2GB)",
                null
        ));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(BusinessRuleException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getCode() != null ? ex.getCode() : "BUSINESS_RULE_VIOLATION");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    @ExceptionHandler(TenancyViolationException.class)
    public ResponseEntity<Map<String, Object>> handleTenancyViolation(TenancyViolationException ex) {
        log.warn("Tenancy violation attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON).body(errorBody(
                403,
                "Không tìm thấy dữ liệu",
                null
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON).body(errorBody(
                403,
                ex.getMessage(),
                null
        ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body(errorBody(
                401,
                "Yêu cầu xác thực tài khoản",
                null
        ));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex) {
        log.error("[409] DataIntegrityViolationException thrown: {}", ex.getMessage(), ex);
        if (containsMessage(ex, "uk_teacher_reviews_submission_attempt_id")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(errorBody(
                    409,
                    "Phiếu chấm cho lượt nộp này đã tồn tại. Vui lòng tải lại trang.",
                    null
            ));
        }
        if (containsMessage(ex, "uq_class_enrollments_class_student")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(conflictBody(
                    "ENROLLMENT_ALREADY_EXISTS",
                    "Học viên đã có hồ sơ ghi danh trong lớp này."
            ));
        }
        if (containsMessage(ex, "uq_fee_records_student_class_month")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(conflictBody(
                    "FEE_RECORD_ALREADY_EXISTS",
                    "Học phí của học viên cho lớp và kỳ này đã tồn tại."
            ));
        }
        if (containsMessage(ex, "Duplicate entry")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(conflictBody(
                    "DATA_DUPLICATE",
                    "Dữ liệu đã tồn tại hoặc thao tác vừa được xử lý trước đó."
            ));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(errorBody(
                409,
                "Lỗi ràng buộc dữ liệu: Không thể xóa hoặc sửa đổi vì dữ liệu này đang được liên kết bởi các bản ghi khác.",
                null
        ));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotWritableException.class)
    public ResponseEntity<Map<String, Object>> handleNotWritable(org.springframework.http.converter.HttpMessageNotWritableException ex) {
        log.error("HttpMessageNotWritableException thrown: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body(errorBody(
                500,
                "Lỗi định dạng dữ liệu phản hồi",
                null
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        String traceId = java.util.UUID.randomUUID().toString();
        log.error("Unhandled exception [traceId={}]: ", traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body(errorBody(
                500,
                "Lỗi hệ thống nội bộ",
                java.util.Map.of("traceId", traceId)
        ));
    }

    @ExceptionHandler(BulkTeacherValidationException.class)
    public ResponseEntity<Map<String, Object>> handleBulkTeacherValidation(BulkTeacherValidationException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", 400);
        body.put("message", "Kiểm tra dữ liệu giáo viên hàng loạt thất bại");
        body.put("errors", ex.getErrors());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    @ExceptionHandler(BulkStudentValidationException.class)
    public ResponseEntity<Map<String, Object>> handleBulkStudentValidation(BulkStudentValidationException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", 400);
        body.put("message", "Kiểm tra dữ liệu học sinh hàng loạt thất bại");
        body.put("errors", ex.getErrors());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private Map<String, Object> errorBody(int status, String message, Map<String, String> errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status);
        body.put("message", message);
        if (errors != null && !errors.isEmpty()) {
            body.put("errors", errors);
        }
        return body;
    }

    private Map<String, Object> conflictBody(String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", 409);
        body.put("code", code);
        body.put("message", message);
        return body;
    }

    private boolean containsMessage(Throwable throwable, String needle) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(needle)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
