package io.goodforgod.testcontainers.extensions.arangodb;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.testcontainers.arangodb.containers.ArangoContainer;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
final class ArangoContext implements ContainerContext<ArangoConnection> {

    private static final String EXTERNAL_TEST_ARANGODB_HOST = "EXTERNAL_TEST_ARANGODB_HOST";
    private static final String EXTERNAL_TEST_ARANGODB_PORT = "EXTERNAL_TEST_ARANGODB_PORT";
    private static final String EXTERNAL_TEST_ARANGODB_USERNAME = "EXTERNAL_TEST_ARANGODB_USERNAME";
    private static final String EXTERNAL_TEST_ARANGODB_PASSWORD = "EXTERNAL_TEST_ARANGODB_PASSWORD";

    private volatile ArangoConnectionImpl connection;

    private final ArangoContainer container;

    ArangoContext(ArangoContainer container) {
        this.container = container;
    }

    @Override
    public @NotNull ArangoConnection connection() {
        if (connection == null) {
            final Optional<ArangoConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !container.isRunning()) {
                throw new IllegalStateException("ArangoConnection can't be create for container that is not running");
            }

            final ArangoConnection containerConnection = connectionExternal.orElseGet(() -> {
                final String alias = container.getNetworkAliases().get(container.getNetworkAliases().size() - 1);
                return ArangoConnectionImpl.forContainer(container.getHost(),
                        container.getMappedPort(ArangoContainer.PORT),
                        alias,
                        ArangoContainer.PORT,
                        container.getUser(),
                        container.getPassword());
            });

            this.connection = (ArangoConnectionImpl) containerConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<ArangoConnection> connectionExternal = getConnectionExternal();
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
    private static Optional<ArangoConnection> getConnectionExternal() {
        var host = System.getenv(EXTERNAL_TEST_ARANGODB_HOST);
        var port = System.getenv(EXTERNAL_TEST_ARANGODB_PORT);
        var username = Optional.ofNullable(System.getenv(EXTERNAL_TEST_ARANGODB_USERNAME)).orElse("root");
        var password = System.getenv(EXTERNAL_TEST_ARANGODB_PASSWORD);

        if (host != null && port != null) {
            return Optional.of(ArangoConnectionImpl.forExternal(host, Integer.parseInt(port), username, password));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return container.getDockerImageName();
    }
}
