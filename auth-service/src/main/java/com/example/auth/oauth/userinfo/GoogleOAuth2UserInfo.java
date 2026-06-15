package com.example.auth.oauth.userinfo;

import java.util.Map;

public class GoogleOAuth2UserInfo extends OAuth2UserInfo {

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    // 구글이 부여한 사용자 식별 번호
    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    // 사용자 이메일
    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    // 사용자의 전체 이름 (성 + 이름)
    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    // 프로필 사진 이미지 경로
    @Override
    public String getPictureUrl() {
        return (String) attributes.get("picture");
    }
}
