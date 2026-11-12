package com.example.cafe.api.dto;

/** PATCH /api/orders/{id}/status 본문 */
public record ChangeStatusRequest(String status) {
}
