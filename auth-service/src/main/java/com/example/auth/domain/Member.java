package com.example.auth.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Member {

    @Id @GeneratedValue
    private Long id;

    private String email;
    private String name;
    private String picture;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    private String providerId;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    public void update(String name, String email, String picture) {
        this.name = name;
        this.email = email;
        this.picture = picture;
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }
}
