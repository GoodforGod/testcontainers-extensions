package io.goodforgod.testcontainers.extensions.rustfs;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;

@TestcontainersRustFS(mode = ContainerMode.PER_CLASS,
        bucket = @Bucket(
                create = Bucket.Mode.PER_METHOD,
                drop = Bucket.Mode.PER_METHOD,
                value = { "bucketto" }))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RustFSPerMethodBucketTests {

    @BeforeEach
    public void setupEach(@ConnectionRustFS RustFSConnection paramConnection) throws Exception {
        assertNotNull(paramConnection);
        assertTrue(paramConnection.client().bucketExists(BucketExistsArgs.builder().bucket("bucketto").build()));
    }

    @Order(1)
    @Test
    void firstRun(@ConnectionRustFS RustFSConnection paramConnection) throws Exception {
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
    void secondRun(@ConnectionRustFS RustFSConnection paramConnection) throws Exception {
        assertNotNull(paramConnection);
        assertTrue(paramConnection.client().bucketExists(BucketExistsArgs.builder().bucket("bucketto").build()));

        ErrorResponseException ex = assertThrows(ErrorResponseException.class,
                () -> paramConnection.client().getObject(GetObjectArgs.builder()
                        .bucket("bucketto")
                        .object("someObj")
                        .build()));
        assertEquals(404, ex.response().code());
    }
}
