package io.goodforgod.testcontainers.extensions.rustfs;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.minio.MakeBucketArgs;
import org.junit.jupiter.api.Test;

@TestcontainersRustFS(mode = ContainerMode.PER_CLASS)
class RustFSConnectionAssertsTests {

    @ConnectionRustFS
    private RustFSConnection connection;

    @Test
    void assertCountsAtLeastWhenEquals() throws Exception {
        connection.client().makeBucket(MakeBucketArgs.builder()
                .bucket("my--test-bucket")
                .build());
    }
}
