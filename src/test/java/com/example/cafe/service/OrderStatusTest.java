package com.example.cafe.service;

import org.junit.jupiter.api.Test;

import static com.example.cafe.service.OrderStatus.CANCELED;
import static com.example.cafe.service.OrderStatus.COMPLETED;
import static com.example.cafe.service.OrderStatus.PREPARING;
import static com.example.cafe.service.OrderStatus.RECEIVED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 상태 전이표 (docs/domain.md) 그대로 */
class OrderStatusTest {

    @Test
    void 허용_전이() {
        assertTrue(RECEIVED.canChangeTo(PREPARING));
        assertTrue(RECEIVED.canChangeTo(CANCELED));
        assertTrue(PREPARING.canChangeTo(COMPLETED));
        assertTrue(PREPARING.canChangeTo(CANCELED));
    }

    @Test
    void 거부_전이() {
        assertFalse(RECEIVED.canChangeTo(RECEIVED));
        assertFalse(RECEIVED.canChangeTo(COMPLETED));
        assertFalse(PREPARING.canChangeTo(RECEIVED));
        assertFalse(PREPARING.canChangeTo(PREPARING));
        for (OrderStatus next : OrderStatus.values()) {
            assertFalse(COMPLETED.canChangeTo(next));
            assertFalse(CANCELED.canChangeTo(next));
        }
    }

    @Test
    void 모르는_상태는_400() {
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.parse("DONE"));
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.parse(""));
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.parse(null));
    }
}
