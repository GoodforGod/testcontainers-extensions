package io.goodforgod.testcontainers.extensions.kafka;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.kafka.KafkaContainer;

@Internal
final class KafkaContext implements ContainerContext<KafkaConnection> {

    static final class KafkaConnectionPool {

        private final List<KafkaConnectionImpl> connections = new ArrayList<>();

        void add(KafkaConnectionImpl connection) {
            connections.add(connection);
        }

        void clear() {
            for (KafkaConnectionImpl connection : connections) {
                try {
                    connection.clear();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }

        void close() {
            for (KafkaConnectionImpl connection : connections) {
                try {
                    connection.stop();
                } catch (Exception e) {
                    // do nothing
                }
            }

            connections.clear();
        }
    }

    private static final String EXTERNAL_TEST_KAFKA_BOOTSTRAP = "EXTERNAL_TEST_KAFKA_BOOTSTRAP_SERVERS";
    private static final String EXTERNAL_TEST_KAFKA_PREFIX = "EXTERNAL_TEST_KAFKA_";

    private final KafkaConnectionPool pool = new KafkaConnectionPool();
    private final GenericContainer<?> container;

    private volatile KafkaConnectionImpl connection;

    KafkaContext(GenericContainer container) {
        this.container = container;
    }

    @NotNull
    public KafkaConnection connection() {
        if (connection == null) {
            final Optional<KafkaConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !container.isRunning()) {
                throw new IllegalStateException("KafkaConnection can't be create for container that is not running");
            }

            final KafkaConnection containerConnection = connectionExternal.orElseGet(() -> {
                final String alias = container.getNetworkAliases().get(container.getNetworkAliases().size() - 1);

                final Properties properties = new Properties();
                if (container instanceof KafkaContainer kc) {
                    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kc.getBootstrapServers());
                } else if (container instanceof ConfluentKafkaContainer ckc) {
                    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, ckc.getBootstrapServers());
                } else if (container instanceof org.testcontainers.containers.KafkaContainer okc) {
                    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, okc.getBootstrapServers());
                } else {
                    throw new UnsupportedOperationException("Unsupported Kafka container type: " + container.getClass());
                }

                final Properties networkProperties = new Properties();
                networkProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        String.format("%s:%s", alias, KafkaConnectionImpl.KAFKA_PORT));

                return new KafkaConnectionImpl(properties, networkProperties);
            });

            this.connection = (KafkaConnectionImpl) containerConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<KafkaConnection> connectionExternal = getConnectionExternal();
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
    KafkaConnectionPool pool() {
        return pool;
    }

    @NotNull
    private static Optional<KafkaConnection> getConnectionExternal() {
        var bootstrap = System.getenv(EXTERNAL_TEST_KAFKA_BOOTSTRAP);
        if (bootstrap != null) {
            final Properties properties = new Properties();
            System.getenv().forEach((k, v) -> {
                if (k.startsWith(EXTERNAL_TEST_KAFKA_PREFIX)) {
                    var name = k.replace(EXTERNAL_TEST_KAFKA_PREFIX, "").replace("_", ".").toLowerCase();
                    properties.put(name, v);
                }
            });

            return Optional.of(new KafkaConnectionImpl(properties, null));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return container.getDockerImageName();
    }
}
