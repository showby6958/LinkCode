package com.example.common.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

@Getter
@AllArgsConstructor
public class CustomPrincipal implements Principal {

    private Long userId;
    private String email;
    private String userName;
    private String picture;
    private String role;

    @Override
    public String getName() {
        return userName;
    }
}
