package com.owlexa.owlexabackend.modules.class_management.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.owlexa.owlexabackend.modules.user.entity.Center;
@Entity
@Table(name = "classes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Class {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false) // B1, B2, C1
    private String vstepLevel;

    @Column(name = "max_students", nullable = false)
    private Integer maxStudents;

    @Column(name = "monthly_fee", nullable = false)
    private Double monthlyFee;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private Center center;
}