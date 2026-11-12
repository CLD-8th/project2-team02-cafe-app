package com.example.cafe.api.dto;

import com.example.cafe.service.MenuItem;

/** 메뉴 응답. imageUrl 은 사진 주소, 사진 없으면 null */
public record MenuResponse(Long id, String name, int price, boolean available, String imageUrl) {

    public static MenuResponse from(MenuItem m, String imageUrl) {
        return new MenuResponse(m.getId(), m.getName(), m.getPrice(), m.isAvailable(), imageUrl);
    }
}
