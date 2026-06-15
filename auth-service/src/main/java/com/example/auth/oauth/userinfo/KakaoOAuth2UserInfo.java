//package com.example.auth.oauth.userinfo;
//
//import java.util.Map;
//
//public class KakaoOAuth2UserInfo extends OAuth2UserInfo {
//
//    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
//        super(attributes);
//    }
//
//    @Override
//    public String getId() {
//        return (String) attributes.get("id");
//    }
//
//    @Override
//    public String getEmail() {
//        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
//        return (String) attributes.get("email");
//    }
//
//    @Override
//    public String getName() {
//        Map<String, Object> profile = (Map<String, Object>) ((Map) attributes.get("kakao_account")).get("profile");
//        return (String) profile.get("nickname");
//    }
//}
