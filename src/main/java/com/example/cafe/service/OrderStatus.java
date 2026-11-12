package com.example.cafe.service;

import java.util.Arrays;
import java.util.Set;

/**
 * 주문 상태와 허용 전이. 전이 규칙은 이 파일 한 곳에만 둠.
 *
 *   RECEIVED ──▶ PREPARING ──▶ COMPLETED
 *       └──────────┴──▶ CANCELED
 */
public enum OrderStatus {
    RECEIVED,
    PREPARING,
    COMPLETED,
    CANCELED;

    public boolean canChangeTo(OrderStatus next) {
        return switch (this) {
            case RECEIVED -> Set.of(PREPARING, CANCELED).contains(next);
            case PREPARING -> Set.of(COMPLETED, CANCELED).contains(next);
            case COMPLETED, CANCELED -> false;
        };
    }

    /** 요청 문자열 → 상태. 모르는 값이면 400 */
    public static OrderStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("status 가 비어 있음");
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("모르는 상태: " + value));
    }
}
