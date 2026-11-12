package com.example.cafe.api.dto;

/** 가입 · 로그인 본문 */
public record AuthRequest(String username, String password) {
}
