package com.example.cafe.api.dto;

import com.example.cafe.service.AuthService;

/** 로그인 응답. accessToken 을 Authorization: Bearer 헤더에 넣어 보냄 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn, String username, String role) {

    public static TokenResponse from(AuthService.Token t) {
        return new TokenResponse(t.accessToken(), "Bearer", t.expiresIn(), t.username(), t.role().name());
    }
}
