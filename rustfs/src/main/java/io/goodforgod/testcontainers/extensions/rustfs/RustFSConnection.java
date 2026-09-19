package io.goodforgod.testcontainers.extensions.rustfs;

import io.minio.MinioClient;
import java.net.URI;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Describes active RustFS connection of currently running {@link RustFSContainer}.
 * <p>
 * RustFS is S3 compatible, so the S3 API is accessed via the MinIO Java client.
 */
public interface RustFSConnection {

    /**
     * RustFS connection parameters
     */
    interface Params {

        @NotNull
        URI uri();

        String host();

        int port();

        String accessKey();

        String secretKey();
    }

    /**
     * @return connection parameters to container
     */
    @NotNull
    Params params();

    /**
     * @return connection parameters inside docker network, can be useful when one container require
     *             params to connect to RustFS container inside docker network
     */
    @NotNull
    Optional<Params> paramsInNetwork();

    /**
     * @return S3 Client (DO NOT CLOSE)
     */
    @NotNull
    MinioClient client();
}
