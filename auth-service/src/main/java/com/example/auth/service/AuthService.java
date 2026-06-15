package com.example.auth.service;

import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    // OAuth 로그인은 Spring Security가 /oauth2/authorization/{provider} 요청을 가로채고, 이후 콜백까지 전부 필터 체인에서 처리함
    // 일반적인 Controller -> Service 호출 구조로 직접 처리하지 않음


    // RefreshToken 검증
    // AccessToken 재발급
    // Redis 관리 (로그아웃 등)
    // 까지만 구현 ㄱㄱ


}
