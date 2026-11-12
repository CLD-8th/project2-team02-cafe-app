package com.example.cafe.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 회원가입 · 로그인 · 내 정보 */
class AuthApiTest extends ApiTestSupport {

    @Test
    void 가입하면_손님() throws Exception {
        String token = newCustomerToken();
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void 직원_계정은_기동_시_생성() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(staffToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("staff"))
                .andExpect(jsonPath("$.role").value("STAFF"));
    }

    @Test
    void 잘못된_가입_로그인() throws Exception {
        // 중복 username
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'username':'staff','password':'password1'}")))
                .andExpect(status().isBadRequest());
        // 짧은 비밀번호 · 대문자 username
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'username':'newuser','password':'short'}")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'username':'NewUser','password':'password1'}")))
                .andExpect(status().isBadRequest());
        // 틀린 비밀번호
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'username':'staff','password':'wrong-password'}")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
