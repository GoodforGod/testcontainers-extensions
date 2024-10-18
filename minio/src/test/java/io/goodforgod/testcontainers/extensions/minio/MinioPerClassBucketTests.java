package io.goodforgod.testcontainers.extensions.minio;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;

@TestcontainersMinio(mode = ContainerMode.PER_CLASS,
        bucket = @Bucket(
                create = Bucket.Mode.PER_CLASS,
                drop = Bucket.Mode.PER_CLASS,
                value = { "bucketto" }))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MinioPerClassBucketTests {

    @BeforeAll
    public static void setupAll(@ConnectionMinio MinioConnection paramConnection) throws Exception {
        assertNotNull(paramConnection);
        assertTrue(paramConnection.client().bucketExists(BucketExistsArgs.builder().bucket("bucketto").build()));
    }

    @BeforeEach
    public void setupEach(@ConnectionMinio MinioConnection paramConnection) throws Exception {
        assertNotNull(paramConnection);
        assertTrue(paramConnection.client().bucketExists(BucketExistsArgs.builder().bucket("bucketto").build()));
    }

    @Order(1)
    @Test
    void firstRun(@ConnectionMinio MinioConnection paramConnection) throws Exception {
        assertNotNull(paramConnection);
        assertTrue(paramConnection.client().bucketExists(BucketExistsArgs.builder().bucket("bucketto").build()));

        byte[] bytes = "some".getBytes(StandardCharsets.UTF_8);
        paramConnection.client().putObject(PutObjectArgs.builder()
                .bucket("bucketto")
                .object("someObj")
                .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                .build());
    }

    @Order(2)
    @Test
    void secondRun(@ConnectionMinio MinioConnection paramConnection) throws Exception {
        assertNotNull(paramConnection);
        assertTrue(paramConnection.client().bucketExists(BucketExistsArgs.builder().bucket("bucketto").build()));

        GetObjectResponse object = paramConnection.client().getObject(GetObjectArgs.builder()
                .bucket("bucketto")
                .object("someObj")
                .build());
        assertNotNull(object);
    }
}
