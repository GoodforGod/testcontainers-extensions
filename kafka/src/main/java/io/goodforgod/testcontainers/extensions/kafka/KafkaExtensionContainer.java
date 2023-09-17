package io.goodforgod.testcontainers.extensions.kafka;

import io.goodforgod.testcontainers.extensions.ExtensionContainer;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
final class KafkaExtensionContainer implements
        ExtensionContainer<KafkaContainerExtra, KafkaConnection> {

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
                    connection.close();
                } catch (Exception e) {
                    // do nothing
                }
            }

            connections.clear();
        }
    }

    private final KafkaContainerExtra container;
    private final KafkaConnection connection;
    private final KafkaConnectionPool pool = new KafkaConnectionPool();

    KafkaExtensionContainer(KafkaContainerExtra container, KafkaConnection connection) {
        this.container = container;
        this.connection = connection;
        this.pool.add((KafkaConnectionImpl) connection);
    }

    @NotNull
    public KafkaConnectionPool pool() {
        return pool;
    }

    @Override
    public KafkaContainerExtra container() {
        return container;
    }

    @Override
    public KafkaConnection connection() {
        return connection;
    }

    @Override
    public void stop() {
        pool.close();
        container.stop();
    }
}
