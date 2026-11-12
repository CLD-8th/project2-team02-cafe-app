package com.example.cafe.api.dto;

/** 메뉴 추가(POST) · 수정(PATCH) 본문. PATCH 는 넘긴 값만 바뀜 */
public record MenuRequest(String name, Integer price, Boolean available) {
}
