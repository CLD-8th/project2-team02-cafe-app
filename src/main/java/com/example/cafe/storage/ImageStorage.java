package com.example.cafe.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * 메뉴 사진 저장소. file-service 의 FileStorage 와 같은 나눔.
 *   local → LocalImageStorage (디스크)   prod → S3ImageStorage (S3 · 프리사인 GET)
 */
public interface ImageStorage {

    String kind();

    /** 사진을 key 로 저장 */
    void save(String key, InputStream content, long size, String contentType) throws IOException;

    /** 브라우저가 사진을 받을 주소. S3 는 임시 URL, local 은 앱 경로 */
    String url(String key);

    void delete(String key) throws IOException;
}
