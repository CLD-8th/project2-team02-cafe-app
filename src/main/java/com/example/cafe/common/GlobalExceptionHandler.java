package com.example.cafe.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;

/**
 * 전역 예외 처리기.
 *
 * 컨트롤러가 예외를 잡지 않아도 여기서 받아 같은 형태로 변환함.
 * 좁은 예외를 먼저 두고, 마지막에 Exception 을 둠.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        log.debug("대상 부재: {}", e.getMessage());
        return body(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e) {
        return body(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException e) {
        log.debug("권한 없음: {}", e.getMessage());
        return body(HttpStatus.FORBIDDEN, "FORBIDDEN", e.getMessage());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleTransition(InvalidStatusTransitionException e) {
        log.debug("상태 전이 거부: {}", e.getMessage());
        return body(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException e) {
        log.debug("검증 실패: {}", e.getMessage());
        return body(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
    }

    /** 본문이 없거나 JSON 이 깨짐 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        log.debug("본문 읽기 실패: {}", e.getMessage());
        return body(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "요청 본문(JSON)을 읽을 수 없음");
    }

    /** 멀티파트 상한(spring.servlet.multipart.max-file-size)을 넘김. 컨트롤러에 닿기 전에 발생 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleTooLarge(MaxUploadSizeExceededException e) {
        return body(HttpStatus.CONTENT_TOO_LARGE, "PAYLOAD_TOO_LARGE", "사진은 2MB 이하");
    }

    /** multipart 에 file 항목이 없음 */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException e) {
        return body(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "multipart 항목이 없음: " + e.getRequestPartName());
    }

    /** 사진 저장소 입출력 실패. 경로가 드러나지 않게 요약만 */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIo(IOException e) {
        log.error("저장소 입출력 실패", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", "사진 저장 실패");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return body(HttpStatus.NOT_FOUND, "NOT_FOUND", "없는 경로");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException e) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "허용하지 않는 메서드: " + e.getMethod());
    }

    /** 그 밖의 오류. 사유를 그대로 내보내면 내부 정보가 드러나므로 로그에만 남김 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        log.error("처리 중 오류", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류");
    }

    private static ResponseEntity<ErrorResponse> body(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), code, message));
    }
}
