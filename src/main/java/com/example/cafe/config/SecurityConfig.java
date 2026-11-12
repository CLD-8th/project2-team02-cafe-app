package com.example.cafe.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 누가 어디에 들어올 수 있나.
 *
 *   누구나      화면(static) · /actuator/health · info · prometheus · GET /api/menus · 가입 · 로그인
 *   직원만      메뉴 추가 · 수정 · 판매 중지
 *   로그인 필요  그 밖의 /api/** (주문의 손님 · 직원 구분은 OrderService 에서)
 *
 * 세션을 만들지 않음(STATELESS) — Pod 가 여러 개이고 롤링 배포로 바뀌어도 토큰만 있으면 됨.
 * CSRF 는 끔 — 쿠키가 아니라 Authorization 헤더로 인증하므로 해당 없음.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()
                        // local 프로파일의 디스크 사진 (prod 는 S3 프리사인 URL 이라 앱을 거치지 않음)
                        .requestMatchers(HttpMethod.GET, "/local-images/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/menus").permitAll()
                        .requestMatchers("/api/menus", "/api/menus/**").hasRole("STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(unauthorized()))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(unauthorized())
                        .accessDeniedHandler(forbidden()));
        return http.build();
    }

    /** 토큰의 role 클레임(CUSTOMER · STAFF) → 권한 ROLE_CUSTOMER · ROLE_STAFF */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    // 실패 응답도 다른 API 와 같은 {status, code, message, timestamp} 형태
    private AuthenticationEntryPoint unauthorized() {
        return (req, res, ex) -> write(res, 401, "UNAUTHORIZED", "로그인이 필요함");
    }

    private AccessDeniedHandler forbidden() {
        return (req, res, ex) -> write(res, 403, "FORBIDDEN", "권한 없음");
    }

    private static void write(HttpServletResponse res, int status, String code, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"status\":" + status + ",\"code\":\"" + code + "\",\"message\":\"" + message
                + "\",\"timestamp\":\"" + LocalDateTime.now() + "\"}");
    }
}
