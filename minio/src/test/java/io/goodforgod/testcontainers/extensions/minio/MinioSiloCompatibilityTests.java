package io.goodforgod.testcontainers.extensions.minio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

@TestcontainersMinio(image = "pgsty/silo:RELEASE.2026-10-18T00-00-00Z",
        mode = ContainerMode.PER_CLASS,
        bucket = @Bucket(value = "silo-compatibility", create = Bucket.Mode.PER_CLASS, drop = Bucket.Mode.PER_CLASS))
class MinioSiloCompatibilityTests {

    @Test
    void createsBucketAndReadsWrittenObject(@ConnectionMinio MinioConnection connection) throws Exception {
        assertTrue(connection.client().bucketExists(BucketExistsArgs.builder()
                .bucket("silo-compatibility")
                .build()));

        byte[] bytes = "silo-compatible".getBytes(StandardCharsets.UTF_8);
        connection.client().putObject(PutObjectArgs.builder()
                .bucket("silo-compatibility")
                .object("test-object")
                .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                .build());

        try (var object = connection.client().getObject(GetObjectArgs.builder()
                .bucket("silo-compatibility")
                .object("test-object")
                .build())) {
            assertArrayEquals(bytes, object.readAllBytes());
        }
    }
}
