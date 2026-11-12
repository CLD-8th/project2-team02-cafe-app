package com.example.cafe.service;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * 회원 한 명. 데이터베이스 표 users 와 1:1.
 *
 * 실제 개인정보(이메일 · 이름 · 전화번호)는 받지 않음 — username 만.
 * 비밀번호는 BCrypt 해시만 저장함.
 * 엔티티 이름을 AppUser 로 둔 이유 : USER 는 일부 DB 의 예약어.
 */
@Entity(name = "AppUser")
@Table(name = "users")
public class User {

    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9_]{4,20}$");
    public static final int MIN_PASSWORD = 8;
    public static final int MAX_PASSWORD = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String username;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private Instant createdAt;

    protected User() {
        // JPA 용
    }

    public static User create(String username, String passwordHash, Role role) {
        User u = new User();
        u.username = username;
        u.passwordHash = passwordHash;
        u.role = role;
        u.createdAt = Instant.now();
        return u;
    }

    /** 가입 입력 검증. 어기면 400 */
    public static void validate(String username, String rawPassword) {
        if (username == null || !USERNAME.matcher(username).matches()) {
            throw new IllegalArgumentException("username 은 영문 소문자 · 숫자 · _ 로 4~20자");
        }
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD || rawPassword.length() > MAX_PASSWORD) {
            throw new IllegalArgumentException("password 는 " + MIN_PASSWORD + "~" + MAX_PASSWORD + "자");
        }
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
}
