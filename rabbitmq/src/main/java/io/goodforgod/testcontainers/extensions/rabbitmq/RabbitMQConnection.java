package io.goodforgod.testcontainers.extensions.rabbitmq;

import com.rabbitmq.client.ConnectionFactory;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * RabbitMQ Connection to {@link TestcontainersRabbitMQ}
 */
public interface RabbitMQConnection extends AutoCloseable {

    interface Params {

        @NotNull
        String uri();

        @NotNull
        String host();

        int port();

        @NotNull
        String username();

        @NotNull
        String password();

        @NotNull
        String virtualHost();

        @NotNull
        Properties properties();
    }

    record QueueSpec(@NotNull String name, boolean durable, boolean exclusive, boolean autoDelete,
                     @NotNull Map<String, Object> arguments) {

        public QueueSpec {
            arguments = Map.copyOf(arguments);
        }
    }

    record ExchangeSpec(@NotNull String name, @NotNull String type, boolean durable, boolean autoDelete,
                        boolean internal, @NotNull Map<String, Object> arguments) {

        public ExchangeSpec {
            arguments = Map.copyOf(arguments);
        }
    }

    record BindingSpec(@NotNull String queue, @NotNull String exchange, @NotNull String routingKey,
                       @NotNull Map<String, Object> arguments) {

        public BindingSpec {
            arguments = Map.copyOf(arguments);
        }
    }

    record TopologySpec(@NotNull Set<QueueSpec> queues, @NotNull Set<ExchangeSpec> exchanges,
                        @NotNull Set<BindingSpec> bindings) {

        public TopologySpec {
            queues = Set.copyOf(queues);
            exchanges = Set.copyOf(exchanges);
            bindings = Set.copyOf(bindings);
        }

        public boolean isEmpty() {
            return queues.isEmpty() && exchanges.isEmpty() && bindings.isEmpty();
        }
    }

    @NotNull
    Params params();

    @NotNull
    Optional<Params> paramsInNetwork();

    @NotNull
    default RabbitMQConnection withProperties(@NotNull Map<String, String> properties) {
        final Properties props = new Properties();
        props.putAll(properties);
        return withProperties(props);
    }

    @NotNull
    RabbitMQConnection withProperties(@NotNull Properties properties);

    default void send(@NotNull String queue, @NotNull Event... events) {
        send("", queue, Arrays.asList(events));
    }

    default void send(@NotNull String exchange, @NotNull String routingKey, @NotNull Event... events) {
        send(exchange, routingKey, Arrays.asList(events));
    }

    void send(@NotNull String exchange, @NotNull String routingKey, @NotNull List<Event> events);

    @NotNull
    Consumer subscribe(@NotNull String queue);

    void declareQueues(@NotNull Set<QueueSpec> queues);

    void declareExchanges(@NotNull Set<ExchangeSpec> exchanges);

    void declareBindings(@NotNull Set<BindingSpec> bindings);

    default void declareTopology(@NotNull TopologySpec topology) {
        declareExchanges(topology.exchanges());
        declareQueues(topology.queues());
        declareBindings(topology.bindings());
    }

    void deleteQueues(@NotNull Set<String> queues);

    void deleteExchanges(@NotNull Set<String> exchanges);

    default void resetTopology(@NotNull TopologySpec topology) {
        deleteQueues(topology.queues().stream().map(QueueSpec::name).collect(java.util.stream.Collectors.toSet()));
        deleteExchanges(topology.exchanges().stream().map(ExchangeSpec::name)
                .filter(name -> !name.isBlank())
                .collect(java.util.stream.Collectors.toSet()));
        declareTopology(topology);
    }

    interface Consumer extends AutoCloseable {

        void reset();

        @NotNull
        List<ReceivedEvent> receivedPreviously();

        @NotNull
        Optional<ReceivedEvent> getReceived(@NotNull Duration timeout);

        @NotNull
        default Optional<ReceivedEvent> getReceived() {
            return getReceived(Duration.ofSeconds(15));
        }

        @NotNull
        ReceivedEvent getReceivedAtLeastOne(@NotNull Duration timeout);

        @NotNull
        default ReceivedEvent getReceivedAtLeastOne() {
            return getReceivedAtLeastOne(Duration.ofSeconds(15));
        }

        @NotNull
        List<ReceivedEvent> getReceivedAtLeast(int expectedEvents, @NotNull Duration timeout);

        @NotNull
        default List<ReceivedEvent> getReceivedAtLeast(int expectedEvents) {
            return getReceivedAtLeast(expectedEvents, Duration.ofSeconds(15));
        }

        void assertReceivedNone(@NotNull Duration timeToWait);

        @NotNull
        ReceivedEvent assertReceivedAtLeastOne(@NotNull Duration timeout);

        @NotNull
        default ReceivedEvent assertReceivedAtLeastOne() {
            return assertReceivedAtLeastOne(Duration.ofSeconds(15));
        }

        @NotNull
        List<ReceivedEvent> assertReceivedAtLeast(int expectedAtLeast, @NotNull Duration timeout);

        @NotNull
        default List<ReceivedEvent> assertReceivedAtLeast(int expectedAtLeast) {
            return assertReceivedAtLeast(expectedAtLeast, Duration.ofSeconds(15));
        }

        @NotNull
        List<ReceivedEvent> assertReceivedEqualsInTime(int expected, @NotNull Duration timeToWait);

        boolean checkReceivedNone(@NotNull Duration timeToWait);

        default boolean checkReceivedAtLeast(int expectedAtLeast) {
            return checkReceivedAtLeast(expectedAtLeast, Duration.ofSeconds(15));
        }

        boolean checkReceivedAtLeast(int expectedAtLeast, @NotNull Duration timeout);

        boolean checkReceivedEqualsInTime(int expected, @NotNull Duration timeToWait);

        @Override
        void close();
    }

    @NotNull
    static RabbitMQConnection forContainer(@NotNull RabbitMQContainer container) {
        if (!container.isRunning()) {
            throw new IllegalStateException(container.getClass().getSimpleName() + " container is not running");
        }

        final Properties properties = new Properties();
        properties.put(RabbitMQConnectionImpl.PROP_URI, container.getAmqpUrl());
        properties.put(RabbitMQConnectionImpl.PROP_HOST, container.getHost());
        properties.put(RabbitMQConnectionImpl.PROP_PORT, String.valueOf(container.getMappedPort(RabbitMQConnectionImpl.RABBITMQ_PORT)));
        properties.put(RabbitMQConnectionImpl.PROP_USERNAME, "guest");
        properties.put(RabbitMQConnectionImpl.PROP_PASSWORD, "guest");
        properties.put(RabbitMQConnectionImpl.PROP_VIRTUAL_HOST, "/");

        final Properties networkProperties = new Properties();
        networkProperties.put(RabbitMQConnectionImpl.PROP_URI,
                RabbitMQConnectionImpl.buildUri(container.getNetworkAliases().get(0), RabbitMQConnectionImpl.RABBITMQ_PORT,
                        "guest", "guest", "/"));
        networkProperties.put(RabbitMQConnectionImpl.PROP_HOST, container.getNetworkAliases().get(0));
        networkProperties.put(RabbitMQConnectionImpl.PROP_PORT, String.valueOf(RabbitMQConnectionImpl.RABBITMQ_PORT));
        networkProperties.put(RabbitMQConnectionImpl.PROP_USERNAME, "guest");
        networkProperties.put(RabbitMQConnectionImpl.PROP_PASSWORD, "guest");
        networkProperties.put(RabbitMQConnectionImpl.PROP_VIRTUAL_HOST, "/");

        return new RabbitMQConnectionClosableImpl(properties, networkProperties);
    }

    @NotNull
    static RabbitMQConnection forURI(@NotNull String uri) {
        final Properties properties = new Properties();
        properties.put(RabbitMQConnectionImpl.PROP_URI, uri);
        return new RabbitMQConnectionClosableImpl(properties, null);
    }

    @NotNull
    static RabbitMQConnection forURI(@NotNull URI uri) {
        return forURI(uri.toString());
    }

    /**
     * @param properties are {@link ConnectionFactory} properties
     * @return RabbitMQ connection
     */
    @NotNull
    static RabbitMQConnection forProperties(@NotNull Properties properties) {
        return new RabbitMQConnectionClosableImpl(properties, null);
    }

    @Override
    void close();
}