package com.example.cafe.api;

import com.example.cafe.api.dto.AuthRequest;
import com.example.cafe.api.dto.TokenResponse;
import com.example.cafe.api.dto.UserResponse;
import com.example.cafe.service.AuthService;
import com.example.cafe.service.CurrentUser;
import com.example.cafe.service.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원.
 *   POST /api/auth/signup  손님 가입 (누구나)
 *   POST /api/auth/login   로그인 → JWT (누구나)
 *   GET  /api/auth/me      내 정보 (로그인 필요)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@RequestBody AuthRequest req) {
        User user = authService.signup(req.username(), req.password());
        return new UserResponse(user.getId(), user.getUsername(), user.getRole().name());
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody AuthRequest req) {
        return TokenResponse.from(authService.login(req.username(), req.password()));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        CurrentUser me = CurrentUsers.from(jwt);
        return new UserResponse(me.id(), me.username(), me.role().name());
    }
}
