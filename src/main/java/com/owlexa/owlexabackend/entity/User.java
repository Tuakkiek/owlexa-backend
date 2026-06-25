package com.owlexa.owlexabackend.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<UserPermission> userPermissions = new HashSet<>();

    // Helper
    public void grantPermission(Permission permission) {
        UserPermission link = new UserPermission();
        link.setUser(this);
        link.setPermission(permission);
        userPermissions.add(link);
    }

    public void revokePermission(String permissionCode) {
        userPermissions.removeIf(link ->
                link.getPermission() != null
                        && link.getPermission().getCode() !=null
                        && link.getPermission().getCode().equalsIgnoreCase(permissionCode)
        );
    }
}