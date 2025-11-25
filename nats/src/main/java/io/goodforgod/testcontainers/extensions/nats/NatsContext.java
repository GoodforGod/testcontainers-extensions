package io.goodforgod.testcontainers.extensions.nats;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.nats.client.Options;
import io.testcontainers.nats.NatsContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;

@Internal
final class NatsContext implements ContainerContext<NatsConnection> {

    static final class NatsConnectionPool {

        private final List<NatsConnectionImpl> connections = new ArrayList<>();

        void add(NatsConnectionImpl connection) {
            connections.add(connection);
        }

        void clear() {
            for (NatsConnectionImpl connection : connections) {
                try {
                    connection.clear();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }

        void close() {
            for (NatsConnectionImpl connection : connections) {
                try {
                    connection.stop();
                } catch (Exception e) {
                    // do nothing
                }
            }

            connections.clear();
        }
    }

    private static final String EXTERNAL_TEST_NATS_URL = "EXTERNAL_TEST_NATS_URL_SERVERS";
    private static final String EXTERNAL_TEST_NATS_PREFIX = "EXTERNAL_TEST_NATS_";

    private final NatsConnectionPool pool = new NatsConnectionPool();
    private final GenericContainer<?> container;

    private volatile NatsConnectionImpl connection;

    NatsContext(GenericContainer container) {
        this.container = container;
    }

    @NotNull
    public NatsConnection connection() {
        if (connection == null) {
            final Optional<NatsConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !container.isRunning()) {
                throw new IllegalStateException("NatsConnection can't be create for container that is not running");
            }

            final NatsConnection containerConnection = connectionExternal.orElseGet(() -> {
                final String alias = container.getNetworkAliases().get(container.getNetworkAliases().size() - 1);

                final Properties properties = new Properties();
                if (container instanceof NatsContainer kc) {
                    properties.put(Options.PROP_URL, kc.getURI().toString());
                } else {
                    throw new UnsupportedOperationException("Unsupported Nats container type: " + container.getClass());
                }

                final Properties networkProperties = new Properties();
                networkProperties.put(Options.PROP_URL,
                        String.format("%s:%s", alias, NatsContainer.PORT_CLIENT));

                return new NatsConnectionImpl(properties, networkProperties);
            });

            this.connection = (NatsConnectionImpl) containerConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<NatsConnection> connectionExternal = getConnectionExternal();
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
    NatsConnectionPool pool() {
        return pool;
    }

    @NotNull
    private static Optional<NatsConnection> getConnectionExternal() {
        var url = System.getenv(EXTERNAL_TEST_NATS_URL);
        if (url != null) {
            final Properties properties = new Properties();
            System.getenv().forEach((k, v) -> {
                if (k.startsWith(EXTERNAL_TEST_NATS_PREFIX)) {
                    var name = k.replace(EXTERNAL_TEST_NATS_PREFIX, "").replace("_", ".").toLowerCase();
                    properties.put(name, v);
                }
            });

            return Optional.of(new NatsConnectionImpl(properties, null));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return container.getDockerImageName();
    }
}
