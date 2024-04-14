package io.goodforgod.testcontainers.extensions.example;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

final class RedisContext implements ContainerContext<RedisConnection> {

    private volatile RedisConnectionImpl connection;

    private final RedisContainer container;

    RedisContext(RedisContainer container) {
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
        container.stop();
    }

    @NotNull
    private static Optional<RedisConnection> getConnectionExternal() {
        return Optional.empty();
    }

    @Override
    public String toString() {
        return container.getDockerImageName();
    }
}
