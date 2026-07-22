package com.owlexa.owlexabackend.modules.payment.entity;

import com.owlexa.owlexabackend.common.context.TenantAware;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import com.owlexa.owlexabackend.common.listener.TenantEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.User;

@Data
@Entity
@Table(name = "payments")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "center_id = :tenantId")
@EntityListeners(TenantEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_number", unique = true, length = 30, updatable = false)
    private String receiptNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_record_id", nullable = false)
    private FeeRecord feeRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private Center center;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User studentUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collected_by_user_id", nullable = false)
    private User collectedByUser;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "sepay_ref")
    private String sepayRef;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.ACTIVE;

    @Column(name = "void_reason", columnDefinition = "TEXT")
    private String voidReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by_user_id")
    private User voidedBy;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @Override
    public Long getCenterId() {
        return center != null ? center.getId() : null;
    }
}
