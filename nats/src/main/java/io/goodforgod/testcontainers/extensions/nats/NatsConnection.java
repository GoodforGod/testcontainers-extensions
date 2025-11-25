package io.goodforgod.testcontainers.extensions.nats;

import io.nats.client.Options;
import io.testcontainers.nats.NatsCluster;
import io.testcontainers.nats.NatsContainer;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

/**
 * Nats Connection to {@link TestcontainersNats}
 * <p>
 * Provides:
 * Producer functionality
 * <a href="https://docs.confluent.io/platform/current/clients/producer.html">NatsProducer</a>
 * Consumer functionality
 * <a href="https://docs.confluent.io/platform/current/clients/consumer.html">NatsConsumer</a>
 */
public interface NatsConnection extends AutoCloseable {

    /**
     * Nats connection parameters
     */
    interface Params {

        @NotNull
        String url();

        /**
         * @return all nats connection properties used for {@link Consumer} or Producer
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
    default NatsConnection withProperties(@NotNull Map<String, String> properties) {
        final Properties props = new Properties();
        props.putAll(properties);
        return withProperties(props);
    }

    @NotNull
    NatsConnection withProperties(@NotNull Properties properties);

    default void send(@NotNull String subject, @NotNull Event... events) {
        send(subject, Arrays.asList(events));
    }

    /**
     * @param subject to send events
     * @param events  to send in specified subject
     */
    default void send(@NotNull String subject, @NotNull List<Event> events) {
        send(subject, null, events);
    }

    /**
     * @param subject to send events
     * @param events  to send in specified subject
     */
    void send(@NotNull String subject, @Nullable String replyTo, @NotNull List<Event> events);

    /**
     * @param subject to subscribe
     * @return consumer that is subscribed to specified subjects {@link Consumer}
     */
    @NotNull
    Consumer subscribe(@NotNull String subject);

    /**
     * NatsConsumer that is capable of testing/asserting specified subjects
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
    static NatsConnection forContainer(@NotNull NatsContainer container) {
        if (!container.isRunning()) {
            throw new IllegalStateException(container.getClass().getSimpleName() + " container is not running");
        }

        final Properties properties = new Properties();
        properties.put(Options.PROP_URL, container.getURI().toString());

        final Properties networkProperties = new Properties();
        networkProperties.put(Options.PROP_URL,
                String.format("%s:%s", container.getNetworkAliases().get(0), NatsContainer.PORT_CLIENT));

        return new NatsConnectionClosableImpl(properties, networkProperties);
    }

    @NotNull
    static NatsConnection forCluster(@NotNull NatsCluster cluster) {
        if (!cluster.isRunning()) {
            throw new IllegalStateException(cluster.getClass().getSimpleName() + " container is not running");
        }

        final Properties properties = new Properties();
        properties.put(Options.PROP_URL, cluster.getURI().toString());

        final Properties networkProperties = new Properties();
        networkProperties.put(Options.PROP_URL,
                String.format("%s:%s", cluster.getNodes().get(0).getNetworkAliases().get(0), NatsContainer.PORT_CLIENT));

        return new NatsConnectionClosableImpl(properties, networkProperties);
    }

    @NotNull
    static NatsConnection forURI(@NotNull String uri) {
        final Properties properties = new Properties();
        properties.put(Options.PROP_URL, uri);
        return new NatsConnectionClosableImpl(properties, null);
    }

    @NotNull
    static NatsConnection forURI(@NotNull URI uri) {
        return forURI(uri.toString());
    }

    /**
     * @param properties are {@link Options} properties
     * @return nats connection
     */
    @NotNull
    static NatsConnection forProperties(@NotNull Properties properties) {
        return new NatsConnectionClosableImpl(properties, null);
    }

    @Override
    void close();
}
