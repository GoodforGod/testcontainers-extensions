package io.goodforgod.testcontainers.extensions.valkey;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
final class ValkeyContext implements ContainerContext<ValkeyConnection> {

    private static final String EXTERNAL_TEST_VALKEY_USERNAME = "EXTERNAL_TEST_VALKEY_USERNAME";
    private static final String EXTERNAL_TEST_VALKEY_PASSWORD = "EXTERNAL_TEST_VALKEY_PASSWORD";
    private static final String EXTERNAL_TEST_VALKEY_HOST = "EXTERNAL_TEST_VALKEY_HOST";
    private static final String EXTERNAL_TEST_VALKEY_PORT = "EXTERNAL_TEST_VALKEY_PORT";
    private static final String EXTERNAL_TEST_VALKEY_DATABASE = "EXTERNAL_TEST_VALKEY_DATABASE";

    private volatile ValkeyConnectionImpl connection;

    private final ValkeyContainer container;

    ValkeyContext(ValkeyContainer container) {
        this.container = container;
    }

    @NotNull
    public ValkeyConnection connection() {
        if (connection == null) {
            final Optional<ValkeyConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !container.isRunning()) {
                throw new IllegalStateException("ValkeyConnection can't be create for container that is not running");
            }

            final ValkeyConnection containerConnection = connectionExternal.orElseGet(() -> {
                final String alias = container.getNetworkAliases().get(container.getNetworkAliases().size() - 1);
                return ValkeyConnectionImpl.forContainer(container.getHost(),
                        container.getMappedPort(ValkeyContainer.PORT),
                        alias,
                        ValkeyContainer.PORT,
                        container.getDatabase(),
                        container.getUser(),
                        container.getPassword());
            });

            this.connection = (ValkeyConnectionImpl) containerConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<ValkeyConnection> connectionExternal = getConnectionExternal();
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
    private static Optional<ValkeyConnection> getConnectionExternal() {
        var host = System.getenv(EXTERNAL_TEST_VALKEY_HOST);
        var port = System.getenv(EXTERNAL_TEST_VALKEY_PORT);
        var user = System.getenv(EXTERNAL_TEST_VALKEY_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_VALKEY_PASSWORD);
        var database = Optional.ofNullable(System.getenv(EXTERNAL_TEST_VALKEY_DATABASE)).map(Integer::parseInt).orElse(0);

        if (host != null && port != null) {
            return Optional.of(ValkeyConnectionImpl.forExternal(host, Integer.parseInt(port), database, user, password));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return container.getDockerImageName();
    }
}

