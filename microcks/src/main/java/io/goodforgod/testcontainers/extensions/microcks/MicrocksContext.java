package io.goodforgod.testcontainers.extensions.microcks;

import io.github.microcks.testcontainers.MicrocksContainer;
import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
final class MicrocksContext implements ContainerContext<MicrocksConnection> {

    private static final String EXTERNAL_TEST_MICROCKS_HOST = "EXTERNAL_TEST_MICROCKS_HOST";
    private static final String EXTERNAL_TEST_MICROCKS_PORT = "EXTERNAL_TEST_MICROCKS_PORT";
    private static final String EXTERNAL_TEST_MICROCKS_GRPC_PORT = "EXTERNAL_TEST_MICROCKS_GRPC_PORT";

    private volatile MicrocksConnectionImpl connection;

    private final MicrocksContainer container;

    MicrocksContext(MicrocksContainer container) {
        this.container = container;
    }

    @NotNull
    public MicrocksConnection connection() {
        if (connection == null) {
            final Optional<MicrocksConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !container.isRunning()) {
                throw new IllegalStateException("MicrocksConnection can't be create for container that is not running");
            }

            final MicrocksConnection containerConnection = connectionExternal.orElseGet(() -> {
                final String alias = container.getNetworkAliases().get(container.getNetworkAliases().size() - 1);
                return MicrocksConnectionImpl.forContainer(container.getHost(),
                        container.getMappedPort(MicrocksContainer.MICROCKS_HTTP_PORT),
                        container.getMappedPort(MicrocksContainer.MICROCKS_GRPC_PORT),
                        alias,
                        MicrocksContainer.MICROCKS_HTTP_PORT,
                        MicrocksContainer.MICROCKS_GRPC_PORT);
            });

            this.connection = (MicrocksConnectionImpl) containerConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<MicrocksConnection> connectionExternal = getConnectionExternal();
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
    private static Optional<MicrocksConnection> getConnectionExternal() {
        var host = System.getenv(EXTERNAL_TEST_MICROCKS_HOST);
        var port = System.getenv(EXTERNAL_TEST_MICROCKS_PORT);

        if (host != null && port != null) {
            var grpcPort = Optional.ofNullable(System.getenv(EXTERNAL_TEST_MICROCKS_GRPC_PORT))
                    .map(Integer::parseInt)
                    .orElse(MicrocksContainer.MICROCKS_GRPC_PORT);
            return Optional.of(MicrocksConnectionImpl.forExternal(host, Integer.parseInt(port), grpcPort));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return container.getDockerImageName();
    }
}
