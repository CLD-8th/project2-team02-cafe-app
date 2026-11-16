package com.example.cafe.storage;

import com.example.cafe.common.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

/**
 * S3 저장소. prod 프로파일에서만 올라옴. 버킷은 비공개 — 사진은 앱이 읽어서 /images/** 로 내려줌.
 *
 * 버킷 이름은 환경 변수 APP_S3_BUCKET, key 접두사는 menus/ 고정 (Pod 역할 정책의 Resource menus/* 와 짝).
 * 자격 증명은 코드에 없음. 기본 자격 증명 체인이 Pod Identity 를 찾아 씀.
 */
@Component
@Profile("prod")
public class S3ImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(S3ImageStorage.class);

    private final S3Client s3;
    private final String bucket;

    public S3ImageStorage(@Value("${app.s3.bucket}") String bucket,
                          @Value("${app.s3.region}") String region) {
        this.s3 = S3Client.builder().region(Region.of(region)).build();
        this.bucket = bucket;
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

    @Override
    public StoredImage load(String key) {
        try {
            ResponseInputStream<GetObjectResponse> in =
                    s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
            GetObjectResponse meta = in.response();
            return new StoredImage(in, meta.contentLength(), meta.contentType());
        } catch (NoSuchKeyException e) {
            throw new NotFoundException("없는 사진: " + key);
        }
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
