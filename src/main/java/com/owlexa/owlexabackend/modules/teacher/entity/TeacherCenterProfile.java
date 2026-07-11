package com.owlexa.owlexabackend.modules.teacher.entity;

import com.owlexa.owlexabackend.common.context.TenantAware;
import com.owlexa.owlexabackend.common.listener.TenantEntityListener;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Dữ liệu riêng theo từng center của TEACHER.
 *
 * Tại sao cần bảng riêng:
 * - salary là dữ liệu theo từng center, không phải dữ liệu global của user.
 * - Một TEACHER có thể thuộc nhiều center với mức lương khác nhau.
 * - Nếu nhét salary vào User thì sẽ không mô tả được quan hệ này.
 * - Nếu nhét vào Membership thì Membership sẽ phình to không cần thiết
 *   cho STUDENT/OWNER/CASHIER — các role không có salary.
 *
 * Quyền truy cập:
 * - Chỉ OWNER của center tương ứng mới được set/get salary.
 * - TEACHER bình thường không được xem salary (kể cả của mình)
 *   nếu muốn bảo mật tuyệt đối.
 */
@Entity
@Table(
        name = "teacher_center_profile",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_teacher_center_profile_teacher_center",
                columnNames = {"teacher_user_id", "center_id"}
        )
)
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "center_id = :tenantId")
@EntityListeners(TenantEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherCenterProfile implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_user_id", nullable = false)
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "center_id", nullable = false)
    private Center center;

    /**
     * Mức lương theo center. Nullable vì:
     * - Khi OWNER mới thêm teacher vào center, salary có thể chưa được set.
     * - Sau đó OWNER có thể set/update salary qua API riêng.
     *
     * BigDecimal với precision=12, scale=2: tối đa 9.999.999.999,99.
     * Đủ cho mức lương tháng của một giáo viên tại Việt Nam hiện tại.
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal salary;

    /**
     * Đơn vị tiền tệ. Mặc định VND.
     * Lưu riêng để sau này mở rộng cho center nước ngoài.
     */
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "VND";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public Long getCenterId() {
        return center != null ? center.getId() : null;
    }
}