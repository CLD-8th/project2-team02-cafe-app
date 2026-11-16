package com.example.cafe.storage;

import com.example.cafe.common.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * 디스크 저장소. prod 가 아닐 때(내 PC · 테스트). 저장 폴더는 app.storage.local-path (기본 ./data/menu-images).
 */
@Component
@Profile("!prod")
public class LocalImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalImageStorage.class);

    /** 저장할 때 확장자는 Content-Type 에서 정했으므로(ImageValidator) 읽을 때는 확장자로 되돌림 */
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp");

    private final Path root;

    public LocalImageStorage(@Value("${app.storage.local-path:./data/menu-images}") String path) throws IOException {
        this.root = Path.of(path).toAbsolutePath().normalize();
        Files.createDirectories(root);
        log.info("로컬 사진 저장소 : {}", root);
    }

    @Override
    public String kind() {
        return "local";
    }

    @Override
    public void save(String key, InputStream content, long size, String contentType) throws IOException {
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public StoredImage load(String key) throws IOException {
        Path file = resolve(key);
        if (!Files.isRegularFile(file)) {
            throw new NotFoundException("없는 사진: " + key);
        }
        String ext = key.substring(key.lastIndexOf('.') + 1);
        String type = CONTENT_TYPES.getOrDefault(ext, "application/octet-stream");
        return new StoredImage(Files.newInputStream(file), Files.size(file), type);
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(resolve(key));
    }

    /** key 가 root 밖을 가리키지 못하게 함 (../ 차단) */
    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("잘못된 경로");
        }
        return p;
    }
}
