package io.goodforgod.testcontainers.extensions.rustfs;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
final class RustFSContext implements ContainerContext<RustFSConnection> {

    private static final int PORT = 9000;

    private static final String EXTERNAL_TEST_RUSTFS_HOST = "EXTERNAL_TEST_RUSTFS_HOST";
    private static final String EXTERNAL_TEST_RUSTFS_PORT = "EXTERNAL_TEST_RUSTFS_PORT";
    private static final String EXTERNAL_TEST_RUSTFS_ACCESS_KEY = "EXTERNAL_TEST_RUSTFS_ACCESS_KEY";
    private static final String EXTERNAL_TEST_RUSTFS_SECRET_KEY = "EXTERNAL_TEST_RUSTFS_SECRET_KEY";

    private volatile RustFSConnectionImpl connection;

    private final RustFSContainer container;

    RustFSContext(RustFSContainer container) {
        this.container = container;
    }

    @NotNull
    public RustFSConnection connection() {
        if (connection == null) {
            final Optional<RustFSConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !container.isRunning()) {
                throw new IllegalStateException("RustFSConnection can't be create for container that is not running");
            }

            final RustFSConnection containerConnection = connectionExternal.orElseGet(() -> {
                final String alias = container.getNetworkAliases().get(container.getNetworkAliases().size() - 1);
                return RustFSConnectionImpl.forContainer(container.getHost(),
                        container.getMappedPort(PORT),
                        container.getAccessKey(),
                        container.getSecretKey(),
                        alias,
                        PORT);
            });

            this.connection = (RustFSConnectionImpl) containerConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<RustFSConnection> connectionExternal = getConnectionExternal();
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
    private static Optional<RustFSConnection> getConnectionExternal() {
        var host = System.getenv(EXTERNAL_TEST_RUSTFS_HOST);
        var port = System.getenv(EXTERNAL_TEST_RUSTFS_PORT);
        var accessKey = System.getenv(EXTERNAL_TEST_RUSTFS_ACCESS_KEY);
        var secretKey = System.getenv(EXTERNAL_TEST_RUSTFS_SECRET_KEY);

        if (host != null && port != null) {
            return Optional.of(RustFSConnectionImpl.forExternal(host, Integer.parseInt(port), accessKey, secretKey));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return container.getDockerImageName();
    }
}
