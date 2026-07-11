package com.owlexa.owlexabackend.modules.teacher.dto.response;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherResponse {

    private Long userId;
    private String fullName;
    private String phoneNumber;
    private Long centerId;
    private String temporaryPassword;

    /**
     * Mức lương của teacher tại center hiện tại.
     *
     * Lưu ý bảo mật:
     * - Chỉ OWNER của center đó mới thấy field này.
     * - TEACHER/STUDENT/CASHIER gọi API list teacher sẽ nhận salary = null
     *   ngay cả khi DB có giá trị, vì service sẽ lọc theo role.
     *
     * Null khi OWNER chưa set lương cho teacher tại center này.
     */
    private BigDecimal salary;

    /**
     * Đơn vị tiền tệ tương ứng với salary. Mặc định VND.
     * Trả null nếu salary = null (chưa set).
     */
    private String currency;
}