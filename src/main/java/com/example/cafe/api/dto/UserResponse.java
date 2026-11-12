package com.example.cafe.api.dto;

/** 회원 정보 응답. 비밀번호 해시는 내보내지 않음 */
public record UserResponse(Long id, String username, String role) {
}
