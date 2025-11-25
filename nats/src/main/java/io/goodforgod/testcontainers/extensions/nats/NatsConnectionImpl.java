package io.goodforgod.testcontainers.extensions.nats;

import io.nats.client.*;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsMessage;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;

@Internal
class NatsConnectionImpl implements NatsConnection {

    private static final Duration NATS_POLL_TIMEOUT = Duration.ofMillis(25);

    private static final class ParamsImpl implements Params {

        private final String url;
        private final Properties properties;

        private ParamsImpl(Properties properties) {
            this.url = properties.getProperty(Options.PROP_URL);
            this.properties = properties;
        }

        @Override
        public @NotNull String url() {
            return url;
        }

        @Override
        public @NotNull Properties properties() {
            return properties;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ParamsImpl params = (ParamsImpl) o;
            return Objects.equals(url, params.url) && Objects.equals(properties, params.properties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(url, properties);
        }

        @Override
        public String toString() {
            return url;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(NatsConnection.class);

    private volatile boolean isClosed = false;
    private volatile Connection connection;

    private final Map<String, ConsumerImpl> consumerByTopic = new ConcurrentHashMap<>();
    private final ParamsImpl params;
    @Nullable
    private final ParamsImpl paramsInNetwork;

    NatsConnectionImpl(Properties properties, @Nullable Properties propertiesInNetwork) {
        this.params = new ParamsImpl(properties);
        this.paramsInNetwork = (propertiesInNetwork == null)
                ? null
                : new ParamsImpl(propertiesInNetwork);
        this.connection = getConnection(properties);
    }

    static final class ConsumerImpl implements Consumer {

        private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final List<ReceivedEvent> receivedPreviously = new CopyOnWriteArrayList<>();
        private final AtomicBoolean isActive = new AtomicBoolean(true);

        private final Subscription subscription;
        private final BlockingQueue<Message> messageQueue = new LinkedBlockingDeque<>();
        private final String subject;
        private final String clientId;

        ConsumerImpl(Subscription subscription, String clientId, String subject) {
            this.subscription = subscription;
            this.clientId = clientId;
            this.subject = subject;

            logger.trace("NatsConsumer subjects {} poll starting", this.subject);
            poll(Duration.ofMillis(50));
            this.executor.execute(this::launch);
            logger.debug("NatsConsumer subjects {} poll started.", this.subject);
        }

        private void launch() {
            logger.info("NatsConsumer '{}' started consuming events from subjects: {}", clientId, subject);
            while (isActive.get()) {
                try {
                    poll(NATS_POLL_TIMEOUT);
                } catch (Exception e) {
                    logger.error("NatsConsumer '{}' for {} subjects got unhandled exception", clientId, subject, e);
                }
            }
        }

        private void poll(Duration maxPollTimeout) {
            try {
                var message = subscription.nextMessage(maxPollTimeout);
                if (message != null) {
                    logger.info("NatsConsumer '{}' polled '{}' message from subject {}...", clientId, message, subject);

                    message.ack();
                    messageQueue.offer(message);
                } else {
                    logger.trace("NatsConsumer '{}' polled '{}' message from subject {}...", clientId, message, subject);
                }
            } catch (InterruptedException e) {
                // do nothing
            }
        }

        @Override
        public @NotNull List<ReceivedEvent> receivedPreviously() {
            return List.copyOf(receivedPreviously);
        }

        @Override
        public @NotNull Optional<ReceivedEvent> getReceived(@NotNull Duration timeout) {
            try {
                var received = messageQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
                if (received == null) {
                    return Optional.empty();
                }

                var event = new ReceivedEventImpl(received);
                receivedPreviously.add(event);
                return Optional.of(event);
            } catch (InterruptedException e) {
                return Assertions.fail("Expected to receive 1 event, but was interrupted: " + e.getMessage());
            }
        }

        @Override
        public @NotNull ReceivedEvent getReceivedAtLeastOne(@NotNull Duration timeout) {
            return getReceivedAtLeast(1, timeout).get(0);
        }

        @Override
        public @NotNull List<ReceivedEvent> getReceivedAtLeast(int expectedEvents, @NotNull Duration timeout) {
            final List<ReceivedEvent> receivedEvents = new CopyOnWriteArrayList<>();

            final List<Message> drainTo = new ArrayList<>();
            messageQueue.drainTo(drainTo, expectedEvents);
            for (var consumerRecord : drainTo) {
                var event = new ReceivedEventImpl(consumerRecord);
                receivedEvents.add(event);
            }

            if (receivedEvents.size() == expectedEvents) {
                logger.debug("NatsConsumer '{}' received at least {} records from subjects: {}",
                        clientId, receivedEvents.size(), subject);
                receivedPreviously.addAll(receivedEvents);
                return List.copyOf(receivedEvents);
            }

            try {
                Awaitility.await()
                        .atMost(timeout)
                        .pollDelay(Duration.ofMillis(5))
                        .until(() -> {
                            for (int i = 0; i < expectedEvents; i++) {
                                try {
                                    var received = messageQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
                                    if (received == null) {
                                        return receivedEvents;
                                    }

                                    var event = new ReceivedEventImpl(received);
                                    receivedEvents.add(event);
                                } catch (InterruptedException e) {
                                    // do nothing
                                }
                            }
                            return receivedEvents;
                        }, received -> received.size() >= expectedEvents);
            } catch (ConditionTimeoutException e) {
                // do nothing
            }

            logger.debug("NatsConsumer '{}' received at least {} records from subjects: {}",
                    clientId, receivedEvents.size(), subject);
            receivedPreviously.addAll(receivedEvents);
            return List.copyOf(receivedEvents);
        }

        private List<ReceivedEvent> getReceivedEqualsInTime(int expected, @NotNull Duration timeToWait) {
            try {
                Thread.sleep(timeToWait.toMillis());

                final List<ReceivedEvent> receivedEvents = new ArrayList<>();
                final List<Message> drainTo = new ArrayList<>();
                messageQueue.drainTo(drainTo);
                for (var consumerRecord : drainTo) {
                    var event = new ReceivedEventImpl(consumerRecord);
                    receivedEvents.add(event);
                }

                logger.debug("NatsConsumer '{}' received equals {} records in {} from subjects: {}",
                        clientId, receivedEvents.size(), timeToWait, subject);
                receivedPreviously.addAll(receivedEvents);
                return List.copyOf(receivedEvents);
            } catch (InterruptedException e) {
                return Assertions.fail(String.format("Expected to receive %s event, but was interrupted: %s",
                        expected, e.getMessage()));
            }
        }

        @Override
        public void assertReceivedNone(@NotNull Duration timeToWait) {
            if (!checkReceivedNone(timeToWait)) {
                Assertions.fail("Expected to receive 0 events, but received at least 1 event");
            }
        }

        @Override
        public @NotNull ReceivedEvent assertReceivedAtLeastOne(@NotNull Duration timeout) {
            return assertReceivedAtLeast(1, timeout).get(0);
        }

        @Override
        public @NotNull List<ReceivedEvent> assertReceivedAtLeast(int expectedAtLeast, @NotNull Duration timeout) {
            final List<ReceivedEvent> received = getReceivedAtLeast(expectedAtLeast, timeout);
            if (received.size() < expectedAtLeast) {
                return Assertions.fail(String.format("Expected to receive at least %s event, but received %s events",
                        expectedAtLeast, received.size()));
            } else {
                return received;
            }
        }

        @Override
        public @NotNull List<ReceivedEvent> assertReceivedEqualsInTime(int expected, @NotNull Duration timeToWait) {
            final List<ReceivedEvent> received = getReceivedEqualsInTime(expected, timeToWait);
            if (received.size() != expected) {
                return Assertions.fail(String.format("Expected to receive %s event, but received %s events",
                        expected, received.size()));
            } else {
                return received;
            }
        }

        @Override
        public boolean checkReceivedNone(@NotNull Duration timeToWait) {
            try {
                var received = messageQueue.poll(timeToWait.toMillis(), TimeUnit.MILLISECONDS);
                if (received == null) {
                    return true;
                } else {
                    var event = new ReceivedEventImpl(received);
                    receivedPreviously.add(event);
                    return false;
                }
            } catch (InterruptedException e) {
                return Assertions.fail("Expected to receive 0 event, but was interrupted: " + e.getMessage());
            }
        }

        @Override
        public boolean checkReceivedAtLeast(int expectedAtLeast, @NotNull Duration timeout) {
            final List<ReceivedEvent> received = getReceivedAtLeast(expectedAtLeast, timeout);
            return received.size() >= expectedAtLeast;
        }

        @Override
        public boolean checkReceivedEqualsInTime(int expected, @NotNull Duration timeToWait) {
            final List<ReceivedEvent> received = getReceivedEqualsInTime(expected, timeToWait);
            return received.size() == expected;
        }

        @Override
        public void reset() {
            receivedPreviously.clear();
            messageQueue.clear();
        }

        boolean isClosed() {
            return !isActive.get();
        }

        @Override
        public void close() {
            stop();
        }

        void stop() {
            if (isActive.compareAndSet(true, false)) {
                logger.debug("Stopping NatsConsumer '{}' for {} subjects...", clientId, subject);
                final long started = System.nanoTime();

                try {
                    executor.shutdownNow();
                    subscription.unsubscribe();
                    executor.awaitTermination(1, TimeUnit.MINUTES);
                } catch (Exception e) {
                    // do nothing
                } finally {
                    reset();
                    logger.info("Stopped NatsConsumer '{}' for {} subjects took {}", clientId, subject,
                            Duration.ofNanos(System.nanoTime() - started));
                }
            }
        }
    }

    @Override
    public @NotNull Optional<Params> paramsInNetwork() {
        return Optional.ofNullable(paramsInNetwork);
    }

    @Override
    public @NotNull Params params() {
        return params;
    }

    @Override
    public @NotNull NatsConnection withProperties(@NotNull Properties properties) {
        final Properties combinedProperties = new Properties();
        combinedProperties.putAll(params.properties());
        combinedProperties.putAll(properties);

        final Properties networkProperties = paramsInNetwork().map(props -> {
            final Properties natsNetworkProperties = new Properties();
            natsNetworkProperties.putAll(props.properties());
            natsNetworkProperties.putAll(properties);
            return natsNetworkProperties;
        }).orElse(null);

        return new NatsConnectionClosableImpl(combinedProperties, networkProperties);
    }

    @Override
    public void send(@NotNull String subject, @Nullable String replyTo, @NotNull List<Event> events) {
        if (isClosed) {
            throw new NatsConnectionException("Can't send cause was closed");
        }

        for (Event event : events) {
            final Headers headers;
            if (event.headers().isEmpty()) {
                headers = null;
            } else {
                headers = new Headers();
                Map<String, List<Event.Header>> headersByKey = event.headers().stream()
                        .collect(Collectors.groupingBy(Event.Header::key));

                headersByKey.forEach((k, v) -> {
                    List<String> values = v.stream()
                            .map(h -> h.value().asString())
                            .toList();
                    headers.add(k, values);
                });
            }

            try {
                var msgBuilder = NatsMessage.builder()
                        .subject(subject)
                        .data(event.value().asString());

                if (headers != null) {
                    msgBuilder.headers(headers);
                }
                if (replyTo != null) {
                    msgBuilder.replyTo(replyTo);
                }

                logger.trace("NatsProducer sending event: {}", event);
                connection.publish(msgBuilder.build());
                logger.info("NatsProducer sent event to subject '{}' event: {}", subject, event);
            } catch (Exception e) {
                throw new NatsConnectionException("NatsProducer sent event failed: " + event, e);
            }
        }
    }

    @Override
    public @NotNull Consumer subscribe(@NotNull String subject) {
        if (isClosed) {
            throw new NatsConnectionException("Can't subscribed cause was closed");
        }

        try {
            final String id = UUID.randomUUID().toString().substring(0, 8);
            final ConsumerImpl consumer = consumerByTopic.computeIfAbsent(subject, k -> {
                var natsConsumer = getConsumer(subject, id);
                return new ConsumerImpl(natsConsumer, id, subject);
            });

            if (consumer.isClosed()) {
                var natsConsumer = getConsumer(subject, id);
                ConsumerImpl consumerActive = new ConsumerImpl(natsConsumer, id, subject);
                consumerByTopic.put(subject, consumerActive);
                return consumerActive;
            } else {
                return consumer;
            }
        } catch (Exception e) {
            throw new NatsConnectionException("Can't create NatsConsumer", e);
        }
    }

    private static Connection getConnection(Properties properties) {
        try {
            Options opts = Options.builder()
                    .reconnectWait(Duration.ofMillis(500))
                    .maxReconnects(3)
                    .connectionTimeout(Duration.ofSeconds(10))
                    .properties(properties)
                    .build();

            return Nats.connect(opts);
        } catch (InterruptedException | IOException e) {
            throw new NatsConnectionException("Can't create NatsProducer", e);
        }
    }

    private Subscription getConsumer(String subject, @Nullable String queueName) {
        Subscription subscription = (queueName == null)
                ? connection.subscribe(subject)
                : connection.subscribe(subject, queueName);

        return subscription;
    }

    private JetStreamSubscription getJetStreamConsumer(String subject) throws IOException, JetStreamApiException {
        JetStreamOptions jetOptions = JetStreamOptions.builder()
                .requestTimeout(Duration.ofSeconds(10))
                .build();

        PullSubscribeOptions subOptions = PullSubscribeOptions.builder()
                .name("name")
                .stream("stream")
                .build();

        return connection.jetStream(jetOptions).subscribe(subject, subOptions);
    }

    void clear() {
        for (var consumer : consumerByTopic.values()) {
            try {
                consumer.stop();
            } catch (Exception e) {
                // do nothing
            }
        }
        consumerByTopic.clear();
    }

    void stop() {
        if (!isClosed) {
            isClosed = true;

            clear();

            if (connection != null) {
                try {
                    connection.close();
                    connection = null;
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }

    @Override
    public void close() {
        // do nothing
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NatsConnectionImpl that = (NatsConnectionImpl) o;
        return Objects.equals(params, that.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(params);
    }

    @Override
    public String toString() {
        return params.url();
    }
}
