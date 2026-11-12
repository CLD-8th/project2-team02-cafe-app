package com.example.cafe.common;

import com.example.cafe.service.OrderStatus;

/** 허용하지 않는 상태 전이. 전역 처리기에서 409 로 변환됨 */
public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(OrderStatus from, OrderStatus to) {
        super(from + " 에서 " + to + " 로 바꿀 수 없음");
    }
}
