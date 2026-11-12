package com.example.cafe.api.dto;

/** POST /api/orders 본문. 값 검증은 OrderService · Order.create 에서 함 */
public record CreateOrderRequest(Long menuId, Integer quantity) {
}
