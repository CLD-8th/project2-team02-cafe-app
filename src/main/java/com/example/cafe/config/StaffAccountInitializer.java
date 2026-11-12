package com.example.cafe.config;

import com.example.cafe.service.AuthService;
import com.example.cafe.service.Role;
import com.example.cafe.service.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 기동할 때 직원 계정을 하나 만듦.
 *
 * STAFF_USERNAME · STAFF_PASSWORD 환경 변수가 둘 다 있을 때만. 이미 있으면 건너뜀.
 * prod 는 users 표가 DB 에 있으므로 앱이 재시작돼도 계정이 남음.
 * 주의 : 이미 있으면 건너뛰므로 STAFF_PASSWORD 만 바꿔도 기존 비밀번호는 바뀌지 않음.
 * 앱 여러 개가 동시에 떠서 둘 다 만들려 하면 하나는 username 유일 제약으로 실패 → 로그만 남기고 계속 뜸.
 */
@Component
public class StaffAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StaffAccountInitializer.class);

    private final AuthService authService;
    private final UserRepository users;
    private final String username;
    private final String password;

    public StaffAccountInitializer(AuthService authService, UserRepository users,
                                   @Value("${app.staff.username:}") String username,
                                   @Value("${app.staff.password:}") String password) {
        this.authService = authService;
        this.users = users;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank()) {
            log.warn("STAFF_USERNAME · STAFF_PASSWORD 가 없어 직원 계정을 만들지 않음");
            return;
        }
        if (users.existsByUsername(username)) {
            log.info("직원 계정 이미 있음 : {}", username);
            return;
        }
        try {
            authService.register(username, password, Role.STAFF);
            log.info("직원 계정 생성 : {}", username);
        } catch (RuntimeException e) {
            log.warn("직원 계정 생성 건너뜀 : {}", e.getMessage());
        }
    }
}
