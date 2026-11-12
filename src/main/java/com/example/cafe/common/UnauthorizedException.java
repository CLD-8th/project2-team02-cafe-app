package com.example.cafe.common;

/** 로그인 실패. 전역 처리기에서 401 로 변환됨 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
