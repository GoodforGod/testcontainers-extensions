package io.goodforgod.testcontainers.extensions.minio;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.minio.MakeBucketArgs;
import org.junit.jupiter.api.Test;

@TestcontainersMinio(mode = ContainerMode.PER_CLASS)
class MinioConnectionAssertsTests {

    @ConnectionMinio
    private MinioConnection connection;

    @Test
    void assertCountsAtLeastWhenEquals() throws Exception {
        connection.client().makeBucket(MakeBucketArgs.builder()
                .bucket("my--test-bucket")
                .build());
    }
}
