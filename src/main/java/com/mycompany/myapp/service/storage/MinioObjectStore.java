package com.mycompany.myapp.service.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Kho đối tượng MinIO (S3, path-style). Không tự nối lúc app khởi động —
 * {@link StoredMedia} mở kết nối từ cấu hình đã lưu hoặc từ form Test.
 */
public class MinioObjectStore {

    private final MinioClient client;
    private final String bucket;

    public MinioObjectStore(String endpoint, String bucket, String region, String accessKey, String secretKey) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("cpn.minio.endpoint is required");
        }
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("cpn.minio.bucket is required");
        }
        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("cpn.minio access-key and secret-key are required");
        }
        String url = endpoint.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        this.bucket = bucket.trim();
        String resolvedRegion = region == null || region.isBlank() ? "us-east-1" : region.trim();
        this.client = MinioClient.builder().endpoint(url).region(resolvedRegion).credentials(accessKey.trim(), secretKey).build();
    }

    public boolean bucketExists() {
        try {
            return client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        } catch (Exception e) {
            throw failure("bucketExists", bucket, e);
        }
    }

    public void put(String objectKey, byte[] content, String contentType) {
        String key = requireKey(objectKey);
        byte[] body = content == null ? new byte[0] : content;
        String type = contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
        try (ByteArrayInputStream in = new ByteArrayInputStream(body)) {
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(key).stream(in, body.length, -1).contentType(type).build());
        } catch (Exception e) {
            throw failure("put", key, e);
        }
    }

    public byte[] get(String objectKey) {
        String key = requireKey(objectKey);
        try (InputStream in = client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            return in.readAllBytes();
        } catch (Exception e) {
            throw failure("get", key, e);
        }
    }

    public void delete(String objectKey) {
        String key = requireKey(objectKey);
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            throw failure("delete", key, e);
        }
    }

    private static String requireKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank() || objectKey.startsWith("/") || objectKey.contains("..")) {
            throw new IllegalArgumentException("invalid object key");
        }
        return objectKey.trim();
    }

    private static IllegalStateException failure(String action, String target, Exception e) {
        return new IllegalStateException(action + " " + target + " failed: " + e.getMessage(), e);
    }
}
