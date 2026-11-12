package com.example.cafe.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 메뉴 사진 검사. 어기면 400.
 *   형식 jpg · png · webp (Content-Type 기준)   크기 2MB 이하
 * 저장 key 의 확장자는 사용자가 준 파일명이 아니라 Content-Type 에서 정함 — 경로 문자 차단.
 */
public final class ImageValidator {

    public static final long MAX_BYTES = 2 * 1024 * 1024;

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private ImageValidator() {
    }

    /** 검사하고 저장할 확장자를 돌려줌 */
    public static String extensionOf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("사진 파일이 비어 있음");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("사진은 2MB 이하");
        }
        String ext = EXTENSIONS.get(file.getContentType());
        if (ext == null) {
            throw new IllegalArgumentException("jpg · png · webp 만 올릴 수 있음");
        }
        return ext;
    }
}
