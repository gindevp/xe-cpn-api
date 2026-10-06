package com.mycompany.myapp.service.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** Ghi / đọc / xóa một object thử. Bỏ qua nếu không set CPN_MINIO_ENDPOINT. */
class MinioObjectStoreLiveTest {

    @Test
    void putGetDeleteOnConfiguredBucket() {
        String endpoint = System.getenv("CPN_MINIO_ENDPOINT");
        Assumptions.assumeTrue(endpoint != null && !endpoint.isBlank(), "set CPN_MINIO_* to probe MinIO");
        MinioObjectStore store = new MinioObjectStore(
            endpoint,
            env("CPN_MINIO_BUCKET", "cpn"),
            env("CPN_MINIO_REGION", "us-east-1"),
            required("CPN_MINIO_ACCESS_KEY"),
            required("CPN_MINIO_SECRET_KEY")
        );

        assertThat(store.bucketExists()).isTrue();
        String key = "probe/connectivity-" + System.currentTimeMillis() + ".txt";
        byte[] body = "cpn-minio-ok".getBytes(StandardCharsets.UTF_8);
        try {
            store.put(key, body, "text/plain");
            assertThat(store.get(key)).isEqualTo(body);
        } finally {
            store.delete(key);
        }
        assertThatThrownBy(() -> store.get(key)).isInstanceOf(IllegalStateException.class);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String required(String name) {
        String value = System.getenv(name);
        Assumptions.assumeTrue(value != null && !value.isBlank(), name + " is required");
        return value;
    }
}
