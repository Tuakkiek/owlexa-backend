package com.owlexa.owlexabackend.modules.user.entity;

import com.owlexa.owlexabackend.common.context.TenantAware;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import com.owlexa.owlexabackend.common.listener.TenantEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Column(unique = true)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<UserPermission> userPermissions = new HashSet<>();


    public void grantPermission(Permission permission) {
        UserPermission link = new UserPermission();
        link.setUser(this);
        link.setPermission(permission);
        userPermissions.add(link);
    }

    public void revokePermission(String permissionCode) {
        userPermissions.removeIf(link ->
                link.getPermission() != null
                        && link.getPermission().getCode() != null
                        && link.getPermission().getCode().equalsIgnoreCase(permissionCode)
        );
    }
}
