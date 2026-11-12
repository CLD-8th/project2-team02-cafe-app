package com.example.cafe.service;

/**
 * 지금 요청한 사람. JWT 의 클레임(sub · uid · role)에서 만들어짐 — 요청마다 DB 를 보지 않음.
 */
public record CurrentUser(long id, String username, Role role) {

    public boolean isStaff() {
        return role == Role.STAFF;
    }
}
