package io.goodforgod.testcontainers.extensions.mockserver;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.MockServerContainer;

@Internal
final class MockServerContext implements ContainerContext<MockServerConnection> {

    private static final String EXTERNAL_TEST_MOCKSERVER_HOST = "EXTERNAL_TEST_MOCKSERVER_HOST";
    private static final String EXTERNAL_TEST_MOCKSERVER_PORT = "EXTERNAL_TEST_MOCKSERVER_PORT";

    private volatile MockServerConnectionImpl connection;

    private final MockServerContainer container;

    MockServerContext(MockServerContainer container) {
        this.container = container;
    }

    @NotNull
    public MockServerConnection connection() {
        if (connection == null) {
            final Optional<MockServerConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !container.isRunning()) {
                throw new IllegalStateException("MockServerConnection can't be create for container that is not running");
            }

            final MockServerConnection jdbcConnection = connectionExternal.orElseGet(() -> {
                final String alias = container.getNetworkAliases().get(container.getNetworkAliases().size() - 1);
                return MockServerConnectionImpl.forContainer(container.getHost(),
                        container.getMappedPort(MockServerContainer.PORT),
                        alias,
                        MockServerContainer.PORT);
            });

            this.connection = (MockServerConnectionImpl) jdbcConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<MockServerConnection> connectionExternal = getConnectionExternal();
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
    private static Optional<MockServerConnection> getConnectionExternal() {
        var host = System.getenv(EXTERNAL_TEST_MOCKSERVER_HOST);
        var port = System.getenv(EXTERNAL_TEST_MOCKSERVER_PORT);

        if (host != null && port != null) {
            return Optional.of(MockServerConnectionImpl.forExternal(host, Integer.parseInt(port)));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return container.getDockerImageName();
    }
}
