package com.example.cafe.api;

import com.example.cafe.service.CurrentUser;
import com.example.cafe.service.Role;
import org.springframework.security.oauth2.jwt.Jwt;

/** 검증된 JWT → CurrentUser. 클레임 이름은 AuthService.login 과 같음 */
final class CurrentUsers {

    private CurrentUsers() {
    }

    static CurrentUser from(Jwt jwt) {
        Number uid = jwt.getClaim("uid");
        return new CurrentUser(uid.longValue(), jwt.getSubject(), Role.valueOf(jwt.getClaimAsString("role")));
    }
}
