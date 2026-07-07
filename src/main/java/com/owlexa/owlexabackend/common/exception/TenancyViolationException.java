package com.owlexa.owlexabackend.common.exception;

/**
 * Vi phạm tenant isolation — HTTP 403 Forbidden.
 *
 * Ném khi user cố truy cập resource thuộc center khác với center trong JWT/header.
 * Lý do tách riêng (thay vì dùng AccessDeniedException chung):
 *   - Phân biệt được với "user không có quyền trong center này" (AccessDeniedException)
 *     và "user cố ý truy cập cross-tenant" (TenancyViolationException).
 *   - Dễ log alert: có thể là hacking attempt hoặc bug frontend.
 *   - Frontend hiển thị thông báo khác nhau:
 *     403 thường: "Bạn không có quyền"
 *     403 tenancy: "Resource không tồn tại" (che giấu sự tồn tại để chống enumeration)
 */
public class TenancyViolationException extends RuntimeException {
    public TenancyViolationException(String message) {
        super(message);
    }
}
