package io.goodforgod.testcontainers.extensions.redpanda;

import java.time.Duration;
import java.util.*;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;
import org.testcontainers.redpanda.RedpandaContainer;

/**
 * Redpanda Connection to {@link TestcontainersRedpanda}
 * <p>
 * Provides:
 * Producer functionality
 * <a href="https://docs.confluent.io/platform/current/clients/producer.html">KafkaProducer</a>
 * Consumer functionality
 * <a href="https://docs.confluent.io/platform/current/clients/consumer.html">KafkaConsumer</a>
 */
public interface RedpandaConnection extends AutoCloseable {

    /**
     * Redpanda connection parameters
     */
    interface Params {

        @NotNull
        String bootstrapServers();

        /**
         * @return all redpanda connection properties used for {@link Consumer} or Producer
         */
        @NotNull
        Properties properties();
    }

    @NotNull
    Params params();

    /**
     * @return connection parameters inside docker network, can be useful when one container require
     *             params to connect to Cassandra Database container inside docker network
     */
    @NotNull
    Optional<Params> paramsInNetwork();

    @NotNull
    Admin admin();

    void createTopics(@NotNull Set<String> topics);

    void dropTopics(@NotNull Set<String> topics);

    @NotNull
    default RedpandaConnection withProperties(@NotNull Map<String, String> properties) {
        final Properties props = new Properties();
        props.putAll(properties);
        return withProperties(props);
    }

    @NotNull
    RedpandaConnection withProperties(@NotNull Properties properties);

    void send(@NotNull String topic, @NotNull Event... events);

    /**
     * @param topic  to send events
     * @param events to send in specified topic
     */
    void send(@NotNull String topic, @NotNull List<Event> events);

    @NotNull
    Consumer subscribe(@NotNull String... topics);

    /**
     * @param topics to subscribe
     * @return consumer that is subscribed to specified topics {@link Consumer}
     */
    @NotNull
    Consumer subscribe(@NotNull Set<String> topics);

    /**
     * RedpandaConsumer that is capable of testing/asserting specified topics
     */
    interface Consumer extends AutoCloseable {

        /**
         * Reset consumer state and wipe out all already consumed messages
         */
        void reset();

        /**
         * @return return already consumed previously events in Assert or Check methods
         */
        @NotNull
        List<ReceivedEvent> receivedPreviously();

        /**
         * @param timeout to wait for new event
         * @return try to receive new event or empty if not received
         */
        @NotNull
        Optional<ReceivedEvent> getReceived(@NotNull Duration timeout);

        @NotNull
        default Optional<ReceivedEvent> getReceived() {
            return getReceived(Duration.ofSeconds(15));
        }

        /**
         * @param timeout to wait for new events
         * @return try to receive N specified events as list in specified time
         */
        @NotNull
        ReceivedEvent getReceivedAtLeastOne(@NotNull Duration timeout);

        @NotNull
        default ReceivedEvent getReceivedAtLeastOne() {
            return getReceivedAtLeastOne(Duration.ofSeconds(15));
        }

        /**
         * @param timeout        to wait for new events
         * @param expectedEvents to receive
         * @return try to receive N specified events as list in specified time
         */
        @NotNull
        List<ReceivedEvent> getReceivedAtLeast(int expectedEvents, @NotNull Duration timeout);

        @NotNull
        default List<ReceivedEvent> getReceivedAtLeast(int expectedEvents) {
            return getReceivedAtLeast(expectedEvents, Duration.ofSeconds(15));
        }

        /**
         * @param timeToWait time to check that 0 events received in specified time
         * @throws AssertionFailedError if received any event in specified time {@link Assertions#fail()}
         */
        void assertReceivedNone(@NotNull Duration timeToWait);

        /**
         * @param timeout time to check that at least N events received in specified time
         * @throws AssertionFailedError if received less than N event in specified time
         *                              {@link Assertions#fail()}
         */
        @NotNull
        ReceivedEvent assertReceivedAtLeastOne(@NotNull Duration timeout);

        @NotNull
        default ReceivedEvent assertReceivedAtLeastOne() {
            return assertReceivedAtLeastOne(Duration.ofSeconds(15));
        }

        /**
         * @param expectedAtLeast number of expected events
         * @param timeout         time to check that at least N events received in specified time
         * @throws AssertionFailedError if received less than N event in specified time
         *                              {@link Assertions#fail()}
         */
        @NotNull
        List<ReceivedEvent> assertReceivedAtLeast(int expectedAtLeast, @NotNull Duration timeout);

        @NotNull
        default List<ReceivedEvent> assertReceivedAtLeast(int expectedAtLeast) {
            return assertReceivedAtLeast(expectedAtLeast, Duration.ofSeconds(15));
        }

        /**
         * @param expected   number of expected events
         * @param timeToWait time to wait for exactly N events received in specified time
         * @throws AssertionFailedError if less or more than N event received in specified time
         *                              {@link Assertions#fail()}
         */
        @NotNull
        List<ReceivedEvent> assertReceivedEqualsInTime(int expected, @NotNull Duration timeToWait);

        /**
         * @param timeToWait time to check that 0 events received in specified time
         * @return true if received None or false
         */
        boolean checkReceivedNone(@NotNull Duration timeToWait);

        default boolean checkReceivedAtLeast(int expectedAtLeast) {
            return checkReceivedAtLeast(expectedAtLeast, Duration.ofSeconds(15));
        }

        /**
         * @param expectedAtLeast number of expected events
         * @param timeout         time to check that at least N events received in specified time
         * @return true if received N or more events or false
         */
        boolean checkReceivedAtLeast(int expectedAtLeast, @NotNull Duration timeout);

        /**
         * @param expected   number of expected events
         * @param timeToWait time to wait for exactly N events received in specified time
         * @return true if received exactly N events during specified time frame or false
         */
        boolean checkReceivedEqualsInTime(int expected, @NotNull Duration timeToWait);

        @Override
        void close();
    }

    @NotNull
    static RedpandaConnection forContainer(@NotNull RedpandaContainer container) {
        if (!container.isRunning()) {
            throw new IllegalStateException(container.getClass().getSimpleName() + " container is not running");
        }

        final Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers());

        final Properties networkProperties = new Properties();
        networkProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                String.format("%s:%s", container.getNetworkAliases().get(0), RedpandaConnectionImpl.REDPANDA_PORT));

        return new RedpandaConnectionClosableImpl(properties, networkProperties);
    }

    @NotNull
    static RedpandaConnection forBootstrapServers(@NotNull String bootstrapServers) {
        final Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new RedpandaConnectionClosableImpl(properties, null);
    }

    /**
     * @param properties are {@link ConsumerConfig} properties
     * @return redpanda connection
     */
    @NotNull
    static RedpandaConnection forProperties(@NotNull Properties properties) {
        return new RedpandaConnectionClosableImpl(properties, null);
    }

    @Override
    void close();
}
