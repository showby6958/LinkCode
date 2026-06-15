package com.example.auth.oauth.service;

import com.example.auth.domain.AuthProvider;
import com.example.auth.domain.Member;
import com.example.auth.domain.UserRole;
import com.example.auth.oauth.userinfo.CustomOAuth2User;
import com.example.auth.oauth.userinfo.OAuth2UserInfo;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Value("${system.admin.email}")
    private String adminEmail;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {

        // OAuth 제공자(구글, 네이버, 카카오) API 호출해서 사용자 정보 가져옴
        OAuth2User oAuth2User = super.loadUser(request);

        String registrationId = request.getClientRegistration().getRegistrationId();

        // provider별 파싱 전략 적용
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        // enum으로 변환(DB 저장용) e.g "google" -> "GOOGLE"
        AuthProvider provider;
        try {
            provider = AuthProvider.valueOf(registrationId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OAuth2AuthenticationException("지원하지 않는 OAuth Provider");
        }

        // 기존 유저면 업데이트, 없으면 신규 생성
        Member member = userRepository.findByProviderAndProviderId(provider, userInfo.getId())
                .map(existingMember -> updateUser(existingMember, userInfo))
                .orElseGet(() -> registerUser(userInfo, provider));

        Map<String, Object> attributes = Map.of(
                "userId", member.getId(),
                "email", member.getEmail(),
                "name", member.getName(),
                "picture", userInfo.getPictureUrl(),
                "provider", member.getProvider().name()
        );

        // CustomOAuth2User 반환
        return new CustomOAuth2User(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getPicture(),
                member.getRole(),
                attributes
        );
    }

    // 신규 회원가입
    private Member registerUser(OAuth2UserInfo userInfo, AuthProvider provider) {

        Member member = Member.builder()
                .email(userInfo.getEmail())
                .name(userInfo.getName())
                .picture(userInfo.getPictureUrl())
                .provider(provider)
                .providerId(userInfo.getId())
                .role(UserRole.ROLE_USER)
                .build();

        return userRepository.save(member);
    }

    // 기존 회원 정보 업데이트
    private Member updateUser(Member member, OAuth2UserInfo userInfo) {

        member.update(userInfo.getName(), userInfo.getEmail(), userInfo.getPictureUrl());

        if (isSystemAdmin(userInfo.getEmail())) {
            member.changeRole(UserRole.ROLE_ADMIN);
            log.info("계정 권한 ROLE_ADMIN으로 변경");
        }

        return member;
    }

    private boolean isSystemAdmin(String email) {
        if (email == null || adminEmail == null) return false;

        return adminEmail.equals(email);
    }
}
