package com.example.cafe.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * 메뉴 사진 저장소. local · 테스트는 디스크(LocalImageStorage), prod 는 S3(S3ImageStorage).
 * 사진은 어느 쪽이든 앱이 /images/{key} 로 내려줌 (ImageController) — 브라우저는 저장소를 직접 보지 않음.
 */
public interface ImageStorage {

    /** 브라우저가 사진을 받을 주소의 앞부분. 뒤에 key(menus/{id}/{uuid}.{ext})가 붙음 */
    String URL_PREFIX = "/images/";

    String kind();

    /** 사진을 key 로 저장 */
    void save(String key, InputStream content, long size, String contentType) throws IOException;

    /** 저장한 사진을 읽음. 없으면 NotFoundException (404) */
    StoredImage load(String key) throws IOException;

    void delete(String key) throws IOException;
}
