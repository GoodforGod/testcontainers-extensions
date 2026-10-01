package io.goodforgod.testcontainers.extensions.wiremock;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.wiremock.integrations.testcontainers.WireMockContainer;

@Internal
final class WireMockContext implements ContainerContext<WireMockConnection> {

    private static final int PORT = 8080;

    private static final String EXTERNAL_TEST_WIREMOCK_HOST = "EXTERNAL_TEST_WIREMOCK_HOST";
    private static final String EXTERNAL_TEST_WIREMOCK_PORT = "EXTERNAL_TEST_WIREMOCK_PORT";

    private volatile WireMockConnectionImpl connection;

    private final WireMockContainer container;

    WireMockContext(WireMockContainer container) {
        this.container = container;
    }

    @NotNull
    public WireMockConnection connection() {
        if (connection == null) {
            final Optional<WireMockConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !container.isRunning()) {
                throw new IllegalStateException("WireMockConnection can't be create for container that is not running");
            }

            final WireMockConnection containerConnection = connectionExternal.orElseGet(() -> {
                final String alias = container.getNetworkAliases().get(container.getNetworkAliases().size() - 1);
                return WireMockConnectionImpl.forContainer(container.getHost(),
                        container.getMappedPort(PORT),
                        alias,
                        PORT);
            });

            this.connection = (WireMockConnectionImpl) containerConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<WireMockConnection> connectionExternal = getConnectionExternal();
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
    private static Optional<WireMockConnection> getConnectionExternal() {
        var host = System.getenv(EXTERNAL_TEST_WIREMOCK_HOST);
        var port = System.getenv(EXTERNAL_TEST_WIREMOCK_PORT);

        if (host != null && port != null) {
            return Optional.of(WireMockConnectionImpl.forExternal(host, Integer.parseInt(port)));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return container.getDockerImageName();
    }
}
