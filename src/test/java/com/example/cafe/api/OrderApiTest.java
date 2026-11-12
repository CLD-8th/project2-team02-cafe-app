package com.example.cafe.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 주문 API 4개 + 손님/직원 권한 */
class OrderApiTest extends ApiTestSupport {

    /** 기본 메뉴 1번(아메리카노 4500)으로 주문 */
    private long createOrder(String token, int quantity) throws Exception {
        String res = mvc.perform(post("/api/orders").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'menuId':1,'quantity':" + quantity + "}")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.menuName").value("아메리카노"))
                .andExpect(jsonPath("$.totalPrice").value(4500 * quantity))
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(field(res, "id"));
    }

    private void changeStatus(String token, long id, String to, int expected) throws Exception {
        mvc.perform(patch("/api/orders/" + id + "/status").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'status':'" + to + "'}")))
                .andExpect(status().is(expected));
    }

    @Test
    void 로그인_없이는_401() throws Exception {
        mvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/orders").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 직원이_접수_제조_완료() throws Exception {
        String customer = newCustomerToken();
        String staff = staffToken();
        long id = createOrder(customer, 2);

        changeStatus(staff, id, "PREPARING", 200);
        changeStatus(staff, id, "COMPLETED", 200);
        changeStatus(staff, id, "PREPARING", 409);

        mvc.perform(get("/api/orders/" + id).header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void 손님은_접수_상태에서만_취소() throws Exception {
        String customer = newCustomerToken();
        String staff = staffToken();

        long a = createOrder(customer, 1);
        changeStatus(customer, a, "PREPARING", 403);   // 손님은 취소만
        changeStatus(customer, a, "CANCELED", 200);

        long b = createOrder(customer, 1);
        changeStatus(staff, b, "PREPARING", 200);
        changeStatus(customer, b, "CANCELED", 409);    // 제조 중이면 손님은 취소 불가
        changeStatus(staff, b, "CANCELED", 200);       // 직원은 가능
    }

    @Test
    void 손님은_내_주문만() throws Exception {
        String alice = newCustomerToken();
        String bob = newCustomerToken();
        long aliceOrder = createOrder(alice, 1);
        createOrder(bob, 1);

        mvc.perform(get("/api/orders/" + aliceOrder).header("Authorization", bearer(bob)))
                .andExpect(status().isForbidden());

        // bob 의 목록에는 alice 주문이 없음
        mvc.perform(get("/api/orders").header("Authorization", bearer(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + aliceOrder + ")]").doesNotExist());

        // 직원은 둘 다 봄
        mvc.perform(get("/api/orders").header("Authorization", bearer(staffToken())))
                .andExpect(jsonPath("$[?(@.id == " + aliceOrder + ")]").exists());
    }

    @Test
    void 목록_상태_필터() throws Exception {
        String customer = newCustomerToken();
        long received = createOrder(customer, 1);
        long canceled = createOrder(customer, 1);
        changeStatus(customer, canceled, "CANCELED", 200);

        mvc.perform(get("/api/orders").param("status", "CANCELED").header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + canceled + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + received + ")]").doesNotExist())
                .andExpect(jsonPath("$[*].status").value(everyItem(is("CANCELED"))));
    }

    @Test
    void 잘못된_요청() throws Exception {
        String customer = newCustomerToken();

        mvc.perform(post("/api/orders").header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(json("{'menuId':9999,'quantity':1}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mvc.perform(post("/api/orders").header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(json("{'menuId':1,'quantity':11}")))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/orders").header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/orders").param("status", "DONE").header("Authorization", bearer(customer)))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/orders/999999").header("Authorization", bearer(customer)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void actuator_는_누구나() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
    }
}
