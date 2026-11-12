package com.example.cafe.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * 메뉴 사진 저장소. 지금 구현은 디스크(LocalImageStorage) 하나.
 * 운영 저장소가 정해지면 이 인터페이스의 구현을 추가함.
 */
public interface ImageStorage {

    String kind();

    /** 사진을 key 로 저장 */
    void save(String key, InputStream content, long size, String contentType) throws IOException;

    /** 브라우저가 사진을 받을 주소 */
    String url(String key);

    void delete(String key) throws IOException;
}
