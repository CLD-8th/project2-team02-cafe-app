package com.example.cafe.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 디스크 저장소. prod 가 아닐 때(내 PC · 테스트). AWS 자격 증명 불필요.
 * 저장한 사진은 /local-images/** 로 앱이 직접 내려줌.
 */
@Configuration
@Profile("!prod")
public class LocalImageStorage implements ImageStorage, WebMvcConfigurer {

    public static final String URL_PREFIX = "/local-images/";

    private static final Logger log = LoggerFactory.getLogger(LocalImageStorage.class);

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
    public String url(String key) {
        return URL_PREFIX + key;
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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(URL_PREFIX + "**").addResourceLocations(root.toUri().toString());
    }
}
