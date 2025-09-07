package io.goodforgod.testcontainers.extensions.redpanda;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.redpanda.RedpandaContainer;

@Internal
final class RedpandaContext implements ContainerContext<RedpandaConnection> {

    static final class RedpandaConnectionPool {

        private final List<RedpandaConnectionImpl> connections = new ArrayList<>();

        void add(RedpandaConnectionImpl connection) {
            connections.add(connection);
        }

        void clear() {
            for (RedpandaConnectionImpl connection : connections) {
                try {
                    connection.clear();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }

        void close() {
            for (RedpandaConnectionImpl connection : connections) {
                try {
                    connection.stop();
                } catch (Exception e) {
                    // do nothing
                }
            }

            connections.clear();
        }
    }

    private static final String EXTERNAL_TEST_REDPANDA_BOOTSTRAP = "EXTERNAL_TEST_REDPANDA_BOOTSTRAP_SERVERS";
    private static final String EXTERNAL_TEST_REDPANDA_PREFIX = "EXTERNAL_TEST_REDPANDA_";

    private final RedpandaConnectionPool pool = new RedpandaConnectionPool();
    private final RedpandaContainer container;

    private volatile RedpandaConnectionImpl connection;

    RedpandaContext(RedpandaContainer container) {
        this.container = container;
    }

    @NotNull
    public RedpandaConnection connection() {
        if (connection == null) {
            final Optional<RedpandaConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !container.isRunning()) {
                throw new IllegalStateException("RedpandaConnection can't be create for container that is not running");
            }

            final RedpandaConnection containerConnection = connectionExternal.orElseGet(() -> {
                final String alias = container.getNetworkAliases().get(container.getNetworkAliases().size() - 1);

                final Properties properties = new Properties();
                properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers());

                final Properties networkProperties = new Properties();
                networkProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        String.format("%s:%s", alias, RedpandaConnectionImpl.REDPANDA_PORT));

                return new RedpandaConnectionImpl(properties, networkProperties);
            });

            this.connection = (RedpandaConnectionImpl) containerConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<RedpandaConnection> connectionExternal = getConnectionExternal();
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
        pool.close();
        container.stop();
    }

    @NotNull
    RedpandaContext.RedpandaConnectionPool pool() {
        return pool;
    }

    @NotNull
    private static Optional<RedpandaConnection> getConnectionExternal() {
        var bootstrap = System.getenv(EXTERNAL_TEST_REDPANDA_BOOTSTRAP);
        if (bootstrap != null) {
            final Properties properties = new Properties();
            System.getenv().forEach((k, v) -> {
                if (k.startsWith(EXTERNAL_TEST_REDPANDA_PREFIX)) {
                    var name = k.replace(EXTERNAL_TEST_REDPANDA_PREFIX, "").replace("_", ".").toLowerCase();
                    properties.put(name, v);
                }
            });

            return Optional.of(new RedpandaConnectionImpl(properties, null));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return container.getDockerImageName();
    }
}
