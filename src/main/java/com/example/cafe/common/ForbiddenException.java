package com.example.cafe.common;

/** 권한 없음 (다른 손님의 주문 · 손님이 직원 동작 요청). 전역 처리기에서 403 으로 변환됨 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
