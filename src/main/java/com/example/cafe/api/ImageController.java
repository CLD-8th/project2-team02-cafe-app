package com.example.cafe.api;

import com.example.cafe.common.NotFoundException;
import com.example.cafe.storage.ImageStorage;
import com.example.cafe.storage.StoredImage;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * 메뉴 사진 내려주기. 누구나 (로그인 불필요).
 *   GET /images/menus/{menuId}/{uuid}.{jpg|png|webp}
 * 저장소(디스크 · S3)에서 읽어 그대로 보냄. 사진을 바꾸면 key(uuid)가 바뀌므로
 * 같은 주소의 내용은 변하지 않음 → 1년 캐시 (CloudFront · 브라우저).
 */
@RestController
public class ImageController {

    /** 앱이 만든 key 모양만 받음 — 그 밖의 주소로 저장소를 훑지 못하게 */
    private static final Pattern FILE = Pattern.compile("[0-9a-f-]{36}\\.(jpg|png|webp)");

    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

    private final ImageStorage storage;

    public ImageController(ImageStorage storage) {
        this.storage = storage;
    }

    @GetMapping("/images/menus/{menuId}/{file}")
    public ResponseEntity<InputStreamResource> menuImage(@PathVariable long menuId, @PathVariable String file)
            throws IOException {
        if (!FILE.matcher(file).matches()) {
            throw new NotFoundException("없는 사진: " + file);
        }
        StoredImage image = storage.load("menus/" + menuId + "/" + file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .contentLength(image.size())
                .cacheControl(CACHE)
                .body(new InputStreamResource(image.content()));
    }
}
