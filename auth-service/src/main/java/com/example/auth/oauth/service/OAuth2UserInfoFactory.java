package com.example.auth.oauth.service;


import com.example.auth.oauth.userinfo.GoogleOAuth2UserInfo;
//import com.example.auth.oauth.userinfo.KakaoOAuth2UserInfo;
//import com.example.auth.oauth.userinfo.NaverOAuth2UserInfo;
import com.example.auth.oauth.userinfo.OAuth2UserInfo;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {

        switch (registrationId) {
            case "google":
                return new GoogleOAuth2UserInfo(attributes);
            case "kakao":
//                return new KakaoOAuth2UserInfo(attributes);
            case "naver":
//                return new NaverOAuth2UserInfo(attributes);
            default:
                throw new IllegalArgumentException("지원하지 않는 OAuth provider");
        }
    }
}
