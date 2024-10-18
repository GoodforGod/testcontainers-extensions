package io.goodforgod.testcontainers.extensions.minio;

import io.minio.MinioClient;
import java.net.URI;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Describes active MinIOContainer connection of currently running
 * {@link org.testcontainers.containers.MinIOContainer}
 */
public interface MinioConnection {

    /**
     * MinIOContainer connection parameters
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
     *             params to connect to MinIO container inside docker network
     */
    @NotNull
    Optional<Params> paramsInNetwork();

    /**
     * @return Minio Client (DO NOT CLOSE)
     */
    @NotNull
    MinioClient client();
}
