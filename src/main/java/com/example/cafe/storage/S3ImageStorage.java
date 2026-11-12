package com.example.cafe.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;

/**
 * S3 저장소. prod 프로파일에서만 올라옴. 버킷은 비공개 — 브라우저에는 프리사인 GET URL 을 줌.
 *
 * 버킷 이름은 환경 변수 APP_S3_BUCKET, 접두사는 menus/ 고정 (IAM 정책 Resource 와 짝).
 * 자격 증명은 코드에 없음. 기본 자격 증명 체인이 Pod Identity 를 찾아 씀.
 */
@Component
@Profile("prod")
public class S3ImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(S3ImageStorage.class);

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final Duration ttl;

    public S3ImageStorage(@Value("${app.s3.bucket}") String bucket,
                          @Value("${app.s3.region}") String region,
                          @Value("${app.s3.url-ttl-seconds:600}") long ttlSeconds) {
        this.s3 = S3Client.builder().region(Region.of(region)).build();
        this.presigner = S3Presigner.builder().region(Region.of(region)).build();
        this.bucket = bucket;
        this.ttl = Duration.ofSeconds(ttlSeconds);
        log.info("S3 사진 저장소 : s3://{}/menus/", bucket);
    }

    @Override
    public String kind() {
        return "s3";
    }

    @Override
    public void save(String key, InputStream content, long size, String contentType) {
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket).key(key).contentType(contentType).contentLength(size).build();
        s3.putObject(req, RequestBody.fromInputStream(content, size));
    }

    /** 프리사인 GET URL. 서명자(Pod 의 역할) 권한을 ttl 동안 빌려줌 */
    @Override
    public String url(String key) {
        GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build();
        return presigner.presignGetObject(req).url().toString();
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
