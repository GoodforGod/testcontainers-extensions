package io.goodforgod.testcontainers.extensions.redis;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
final class RedisContext implements ContainerContext<RedisConnection> {

    private static final String EXTERNAL_TEST_REDIS_USERNAME = "EXTERNAL_TEST_REDIS_USERNAME";
    private static final String EXTERNAL_TEST_REDIS_PASSWORD = "EXTERNAL_TEST_REDIS_PASSWORD";
    private static final String EXTERNAL_TEST_REDIS_HOST = "EXTERNAL_TEST_REDIS_HOST";
    private static final String EXTERNAL_TEST_REDIS_PORT = "EXTERNAL_TEST_REDIS_PORT";
    private static final String EXTERNAL_TEST_REDIS_DATABASE = "EXTERNAL_TEST_REDIS_DATABASE";

    private volatile RedisConnectionImpl connection;

    private final RedisContainer<?> container;

    RedisContext(RedisContainer<?> container) {
        this.container = container;
    }

    @NotNull
    public RedisConnection connection() {
        if (connection == null) {
            final Optional<RedisConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !container.isRunning()) {
                throw new IllegalStateException("RedisConnection can't be create for container that is not running");
            }

            final RedisConnection containerConnection = connectionExternal.orElseGet(() -> {
                final String alias = container.getNetworkAliases().get(container.getNetworkAliases().size() - 1);
                return RedisConnectionImpl.forContainer(container.getHost(),
                        container.getMappedPort(RedisContainer.PORT),
                        alias,
                        RedisContainer.PORT,
                        container.getDatabase(),
                        container.getUser(),
                        container.getPassword());
            });

            this.connection = (RedisConnectionImpl) containerConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<RedisConnection> connectionExternal = getConnectionExternal();
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
    private static Optional<RedisConnection> getConnectionExternal() {
        var host = System.getenv(EXTERNAL_TEST_REDIS_HOST);
        var port = System.getenv(EXTERNAL_TEST_REDIS_PORT);
        var user = System.getenv(EXTERNAL_TEST_REDIS_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_REDIS_PASSWORD);
        var database = Optional.ofNullable(System.getenv(EXTERNAL_TEST_REDIS_DATABASE)).map(Integer::parseInt).orElse(0);

        if (host != null && port != null) {
            return Optional.of(RedisConnectionImpl.forExternal(host, Integer.parseInt(port), database, user, password));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return container.getDockerImageName();
    }
}
