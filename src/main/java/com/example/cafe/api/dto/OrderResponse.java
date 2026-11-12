package com.example.cafe.api.dto;

import com.example.cafe.service.Order;

import java.time.Instant;

/** 주문 응답. 메뉴 이름 · 가격은 주문 당시 값 */
public record OrderResponse(Long id, Long menuId, String menuName, int unitPrice, int quantity, int totalPrice,
                            String status, String customer, Instant createdAt) {

    public static OrderResponse from(Order o) {
        return new OrderResponse(o.getId(), o.getMenuId(), o.getMenuName(), o.getUnitPrice(), o.getQuantity(),
                o.getTotalPrice(), o.getStatus().name(), o.getCustomerName(), o.getCreatedAt());
    }
}
