package com.example.cafe.api;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API 테스트 공통. local 프로파일(H2 · 디스크)로 앱 전체를 띄우고 보안 필터까지 통과시킴.
 * 직원 계정(staff / staff1234)은 테스트 설정의 값으로 기동 시 만들어짐 — 가상 데이터.
 */
@SpringBootTest
@ActiveProfiles("local")
abstract class ApiTestSupport {

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUpMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    static String json(String body) {
        return body.replace('\'', '"');
    }

    static String bearer(String token) {
        return "Bearer " + token;
    }

    /** 응답 JSON 에서 "field":값 하나를 꺼냄 (테스트용 간단 파서) */
    static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\":\"?([^\",}]+)").matcher(json);
        if (!m.find()) throw new IllegalStateException(name + " 없음: " + json);
        return m.group(1);
    }

    String login(String username, String password) throws Exception {
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'username':'" + username + "','password':'" + password + "'}")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return field(res, "accessToken");
    }

    String staffToken() throws Exception {
        return login("staff", "staff1234");
    }

    /** 매번 새 손님을 가입시키고 토큰을 돌려줌 */
    String newCustomerToken() throws Exception {
        String username = "c_" + UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'username':'" + username + "','password':'password1'}")))
                .andExpect(status().isCreated());
        return login(username, "password1");
    }
}
