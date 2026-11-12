package com.example.cafe.common;

/** 요청한 주문이 없을 때. 전역 처리기에서 404 로 변환됨 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
