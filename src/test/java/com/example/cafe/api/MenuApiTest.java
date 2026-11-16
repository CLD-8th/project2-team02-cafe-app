package com.example.cafe.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 메뉴 조회 · 직원 관리 · 사진 */
class MenuApiTest extends ApiTestSupport {

    private long createMenu(String staff, String name, int price) throws Exception {
        String res = mvc.perform(post("/api/menus").header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'name':'" + name + "','price':" + price + "}")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(field(res, "id"));
    }

    @Test
    void 기본_메뉴는_누구나_조회() throws Exception {
        mvc.perform(get("/api/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("아메리카노"))
                .andExpect(jsonPath("$[0].price").value(4500));
    }

    @Test
    void 손님은_메뉴_관리_불가() throws Exception {
        String customer = newCustomerToken();
        mvc.perform(post("/api/menus").header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(json("{'name':'모카','price':5500}")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/api/menus").param("all", "true").header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/menus").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'name':'모카','price':5500}")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 직원이_추가_수정_판매중지() throws Exception {
        String staff = staffToken();
        long id = createMenu(staff, "카페모카", 5500);

        mvc.perform(patch("/api/menus/" + id).header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON).content(json("{'price':6000}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(6000))
                .andExpect(jsonPath("$.name").value("카페모카"));

        // 주문해 두고 판매 중지 → 지난 주문은 이름 · 가격 그대로, 새 주문은 400
        String customer = newCustomerToken();
        String order = mvc.perform(post("/api/orders").header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(json("{'menuId':" + id + ",'quantity':1}")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        mvc.perform(delete("/api/menus/" + id).header("Authorization", bearer(staff)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/menus"))
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").doesNotExist());
        mvc.perform(get("/api/menus").param("all", "true").header("Authorization", bearer(staff)))
                .andExpect(jsonPath("$[?(@.id == " + id + " && @.available == false)]").exists());

        mvc.perform(get("/api/orders/" + field(order, "id")).header("Authorization", bearer(customer)))
                .andExpect(jsonPath("$.menuName").value("카페모카"))
                .andExpect(jsonPath("$.unitPrice").value(6000));

        mvc.perform(post("/api/orders").header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(json("{'menuId':" + id + ",'quantity':1}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 잘못된_메뉴_입력() throws Exception {
        String staff = staffToken();
        mvc.perform(post("/api/menus").header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON).content(json("{'name':'아메리카노','price':4500}")))
                .andExpect(status().isBadRequest());   // 중복 이름
        mvc.perform(post("/api/menus").header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON).content(json("{'name':'공짜','price':0}")))
                .andExpect(status().isBadRequest());   // 가격 범위
    }

    @Test
    void 직원이_사진_올리기() throws Exception {
        String staff = staffToken();
        long id = createMenu(staff, "아인슈페너", 6000);
        MockMultipartFile png = new MockMultipartFile("file", "a.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G'});

        String res = mvc.perform(multipart("/api/menus/" + id + "/image").file(png)
                        .with(r -> { r.setMethod("PUT"); return r; })
                        .header("Authorization", bearer(staff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(startsWith("/images/menus/" + id + "/")))
                .andReturn().getResponse().getContentAsString();

        // 사진은 로그인 없이 받을 수 있고, 주소가 바뀌지 않는 한 내용도 같아서 오래 캐시함
        mvc.perform(get(field(res, "imageUrl")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Cache-Control", containsString("max-age=31536000")));

        // 앱이 만든 key 모양이 아니거나 없는 사진은 404
        mvc.perform(get("/images/menus/" + id + "/evil.png")).andExpect(status().isNotFound());
        mvc.perform(get("/images/menus/" + id + "/00000000-0000-0000-0000-000000000000.png"))
                .andExpect(status().isNotFound());

        MockMultipartFile exe = new MockMultipartFile("file", "a.exe", "application/octet-stream", new byte[]{1});
        mvc.perform(multipart("/api/menus/" + id + "/image").file(exe)
                        .with(r -> { r.setMethod("PUT"); return r; })
                        .header("Authorization", bearer(staff)))
                .andExpect(status().isBadRequest());
    }
}
