package com.example.auth.oauth.userinfo;

import java.util.Map;

public class NaverOAuth2UserInfo extends OAuth2UserInfo {

    @SuppressWarnings("unchecked")
    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        super((Map<String, Object>) attributes.get("response"));
    }

    // 네이버가 부여한 사용자 식별 번호
    @Override
    public String getId() {
        return (String) attributes.get("id");
    }

    // 사용자 이메일
    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    // 사용자의 전체 이름
    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    // 프로필 사진 이미지 경로
    @Override
    public String getPictureUrl() {
        return (String) attributes.get("profile_image");
    }
}