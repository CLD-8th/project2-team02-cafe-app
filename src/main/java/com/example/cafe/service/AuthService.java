package com.example.cafe.service;

import com.example.cafe.common.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/** 회원가입 · 로그인(JWT 발급) */
@Service
@Transactional
public class AuthService {

    public record Token(String accessToken, long expiresIn, String username, Role role) {
    }

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Duration ttl;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                       @Value("${app.jwt.ttl-seconds:3600}") long ttlSeconds) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    /** 손님 가입. 직원은 가입으로 만들 수 없음 */
    public User signup(String username, String rawPassword) {
        return register(username, rawPassword, Role.CUSTOMER);
    }

    /** 계정 생성. 같은 username 이 있으면 400 */
    public User register(String username, String rawPassword, Role role) {
        User.validate(username, rawPassword);
        if (users.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 있는 username: " + username);
        }
        return users.save(User.create(username, passwordEncoder.encode(rawPassword), role));
    }

    /** 아이디 · 비밀번호가 맞으면 토큰. 어느 쪽이 틀렸는지는 알려 주지 않음 */
    @Transactional(readOnly = true)
    public Token login(String username, String rawPassword) {
        User user = users.findByUsername(username == null ? "" : username)
                .filter(u -> rawPassword != null && passwordEncoder.matches(rawPassword, u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("아이디 또는 비밀번호가 맞지 않음"));

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cafe")
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new Token(token, ttl.toSeconds(), user.getUsername(), user.getRole());
    }
}
