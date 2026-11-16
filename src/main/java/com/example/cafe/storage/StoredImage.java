package com.example.cafe.storage;

import java.io.InputStream;

/** 저장소에서 읽은 사진. content 는 응답을 다 쓴 뒤 Spring 이 닫음 */
public record StoredImage(InputStream content, long size, String contentType) {
}
