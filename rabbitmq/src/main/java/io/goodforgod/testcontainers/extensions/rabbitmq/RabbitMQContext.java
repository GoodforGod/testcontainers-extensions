package io.goodforgod.testcontainers.extensions.rabbitmq;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;

@Internal
final class RabbitMQContext implements ContainerContext<RabbitMQConnection> {

    static final class RabbitMQConnectionPool {

        private final List<RabbitMQConnectionImpl> connections = new ArrayList<>();

        void add(RabbitMQConnectionImpl connection) {
            connections.add(connection);
        }

        void clear() {
            for (RabbitMQConnectionImpl connection : connections) {
                try {
                    connection.clear();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }

        void close() {
            for (RabbitMQConnectionImpl connection : connections) {
                try {
                    connection.stop();
                } catch (Exception e) {
                    // do nothing
                }
            }

            connections.clear();
        }
    }

    private static final String EXTERNAL_TEST_RABBITMQ_URI = "EXTERNAL_TEST_RABBITMQ_URI";
    private static final String EXTERNAL_TEST_RABBITMQ_PREFIX = "EXTERNAL_TEST_RABBITMQ_";

    private final RabbitMQConnectionPool pool = new RabbitMQConnectionPool();
    private final GenericContainer<?> container;

    private volatile RabbitMQConnectionImpl connection;

    RabbitMQContext(GenericContainer<?> container) {
        this.container = container;
    }

    @Override
    public @NotNull RabbitMQConnection connection() {
        if (connection == null) {
            final Optional<RabbitMQConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !container.isRunning()) {
                throw new IllegalStateException("RabbitMQConnection can't be create for container that is not running");
            }

            final RabbitMQConnection containerConnection = connectionExternal.orElseGet(() -> {
                final String alias = container.getNetworkAliases().get(container.getNetworkAliases().size() - 1);

                final Properties properties = new Properties();
                final Properties networkProperties = new Properties();
                if (container instanceof RabbitMQContainer rabbitContainer) {
                    properties.put(RabbitMQConnectionImpl.PROP_URI, rabbitContainer.getAmqpUrl());
                    properties.put(RabbitMQConnectionImpl.PROP_HOST, rabbitContainer.getHost());
                    properties.put(RabbitMQConnectionImpl.PROP_PORT,
                            String.valueOf(rabbitContainer.getMappedPort(RabbitMQConnectionImpl.RABBITMQ_PORT)));
                    properties.put(RabbitMQConnectionImpl.PROP_USERNAME, "guest");
                    properties.put(RabbitMQConnectionImpl.PROP_PASSWORD, "guest");
                    properties.put(RabbitMQConnectionImpl.PROP_VIRTUAL_HOST, "/");

                    networkProperties.put(RabbitMQConnectionImpl.PROP_URI,
                            RabbitMQConnectionImpl.buildUri(alias, RabbitMQConnectionImpl.RABBITMQ_PORT, "guest", "guest", "/"));
                    networkProperties.put(RabbitMQConnectionImpl.PROP_HOST, alias);
                    networkProperties.put(RabbitMQConnectionImpl.PROP_PORT, String.valueOf(RabbitMQConnectionImpl.RABBITMQ_PORT));
                    networkProperties.put(RabbitMQConnectionImpl.PROP_USERNAME, "guest");
                    networkProperties.put(RabbitMQConnectionImpl.PROP_PASSWORD, "guest");
                    networkProperties.put(RabbitMQConnectionImpl.PROP_VIRTUAL_HOST, "/");
                } else {
                    throw new UnsupportedOperationException("Unsupported RabbitMQ container type: " + container.getClass());
                }

                return new RabbitMQConnectionImpl(properties, networkProperties);
            });

            this.connection = (RabbitMQConnectionImpl) containerConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<RabbitMQConnection> connectionExternal = getConnectionExternal();
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
    RabbitMQConnectionPool pool() {
        return pool;
    }

    @NotNull
    private static Optional<RabbitMQConnection> getConnectionExternal() {
        var uri = System.getenv(EXTERNAL_TEST_RABBITMQ_URI);
        if (uri != null) {
            final Properties properties = new Properties();
            System.getenv().forEach((k, v) -> {
                if (k.startsWith(EXTERNAL_TEST_RABBITMQ_PREFIX)) {
                    var name = k.replace(EXTERNAL_TEST_RABBITMQ_PREFIX, "").replace("_", ".").toLowerCase();
                    properties.put(name, v);
                }
            });

            properties.putIfAbsent(RabbitMQConnectionImpl.PROP_URI, uri);
            return Optional.of(new RabbitMQConnectionImpl(properties, null));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return container.getDockerImageName();
    }
}