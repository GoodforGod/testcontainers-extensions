package io.goodforgod.testcontainers.extensions.minio;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.MinIOContainer;

@Internal
final class MinioContext implements ContainerContext<MinioConnection> {

    private static final int PORT = 9000;

    private static final String EXTERNAL_TEST_MINIO_HOST = "EXTERNAL_TEST_MINIO_HOST";
    private static final String EXTERNAL_TEST_MINIO_PORT = "EXTERNAL_TEST_MINIO_PORT";
    private static final String EXTERNAL_TEST_MINIO_ACCESS_KEY = "EXTERNAL_TEST_MINIO_ACCESS_KEY";
    private static final String EXTERNAL_TEST_MINIO_SECRET_KEY = "EXTERNAL_TEST_MINIO_SECRET_KEY";

    private volatile MinioConnectionImpl connection;

    private final MinIOContainer container;

    MinioContext(MinIOContainer container) {
        this.container = container;
    }

    @NotNull
    public MinioConnection connection() {
        if (connection == null) {
            final Optional<MinioConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !container.isRunning()) {
                throw new IllegalStateException("MockServerConnection can't be create for container that is not running");
            }

            final MinioConnection jdbcConnection = connectionExternal.orElseGet(() -> {
                final String alias = container.getNetworkAliases().get(container.getNetworkAliases().size() - 1);
                return MinioConnectionImpl.forContainer(container.getHost(),
                        container.getMappedPort(PORT),
                        container.getUserName(),
                        container.getPassword(),
                        alias,
                        PORT);
            });

            this.connection = (MinioConnectionImpl) jdbcConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<MinioConnection> connectionExternal = getConnectionExternal();
        if (connectionExternal.isEmpty()) {
            container.start();
        }
    }

    @Override
    public void stop() {
        if (connection != null) {
            connection.stop();
            connection = null;
        }
        container.stop();
    }

    @NotNull
    private static Optional<MinioConnection> getConnectionExternal() {
        var host = System.getenv(EXTERNAL_TEST_MINIO_HOST);
        var port = System.getenv(EXTERNAL_TEST_MINIO_PORT);
        var accessKey = System.getenv(EXTERNAL_TEST_MINIO_ACCESS_KEY);
        var secretKey = System.getenv(EXTERNAL_TEST_MINIO_SECRET_KEY);

        if (host != null && port != null) {
            return Optional.of(MinioConnectionImpl.forExternal(host, Integer.parseInt(port), accessKey, secretKey));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return container.getDockerImageName();
    }
}
