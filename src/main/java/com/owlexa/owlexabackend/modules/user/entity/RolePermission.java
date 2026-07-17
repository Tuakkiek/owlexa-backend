package com.owlexa.owlexabackend.modules.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "role_permission",
        uniqueConstraints = @UniqueConstraint( columnNames = {"role", "permission_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Permission permission;
}
