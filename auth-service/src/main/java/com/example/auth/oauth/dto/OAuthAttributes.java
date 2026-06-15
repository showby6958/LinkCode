package com.example.auth.oauth.dto;

import com.example.auth.domain.AuthProvider;
import com.example.auth.domain.Member;
import com.example.auth.domain.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthAttributes {

    private String email;
    private String name;
    private AuthProvider provider;
    private String providerId;

    public Member toEntity() {
        return Member.builder()
                .email(email)
                .name(name)
                .provider(provider)
                .providerId(providerId)
                .role(UserRole.ROLE_USER)
                .build();
    }
}
