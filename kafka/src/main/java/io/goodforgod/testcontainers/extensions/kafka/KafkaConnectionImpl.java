package io.goodforgod.testcontainers.extensions.kafka;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;

@Internal
class KafkaConnectionImpl implements KafkaConnection {

    private static final Duration POLL_TIMEOUT = Duration.ofMillis(500);

    private static final class ParamsImpl implements Params {

        private final String bootstrapServers;
        private final Properties properties;

        private ParamsImpl(Properties properties) {
            this.bootstrapServers = properties.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG);
            this.properties = properties;
        }

        @Override
        public @NotNull String bootstrapServers() {
            return bootstrapServers;
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
            return Objects.equals(bootstrapServers, params.bootstrapServers) && Objects.equals(properties, params.properties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(bootstrapServers, properties);
        }

        @Override
        public String toString() {
            return bootstrapServers;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(KafkaConnection.class);

    private volatile boolean isClosed = false;
    private volatile KafkaProducer<byte[], byte[]> producer;
    private volatile Admin admin;

    private final List<ConsumerImpl> consumers = new CopyOnWriteArrayList<>();
    private final ParamsImpl params;
    @Nullable
    private final ParamsImpl paramsInNetwork;

    KafkaConnectionImpl(Properties properties, @Nullable Properties propertiesInNetwork) {
        this.params = new ParamsImpl(properties);
        this.paramsInNetwork = (propertiesInNetwork == null)
                ? null
                : new ParamsImpl(propertiesInNetwork);
    }

    static final class ConsumerImpl implements Consumer {

        private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final List<ReceivedEvent> receivedPreviously = new CopyOnWriteArrayList<>();
        private final AtomicBoolean isActive = new AtomicBoolean(true);

        private final KafkaConsumer<byte[], byte[]> consumer;
        private final BlockingQueue<ConsumerRecord<byte[], byte[]>> messageQueue = new LinkedBlockingDeque<>();
        private final Set<String> topics;
        private final String clientId;

        ConsumerImpl(KafkaConsumer<byte[], byte[]> consumer, String clientId, Collection<TopicPartition> topicPartitions) {
            this.consumer = consumer;
            this.clientId = clientId;
            this.topics = topicPartitions.stream().map(TopicPartition::topic).collect(Collectors.toSet());

            logger.trace("KafkaConsumer topics {} assigning...", this.topics);
            this.consumer.assign(topicPartitions);
            Awaitility.await()
                    .atMost(Duration.ofMinutes(1))
                    .pollInterval(Duration.ofMillis(50))
                    .until(() -> {
                        try {
                            return this.consumer.listTopics(Duration.ofSeconds(10));
                        } catch (Exception e) {
                            return Collections.<String, List<PartitionInfo>>emptyMap();
                        }
                    }, result -> new HashSet<>(result.keySet()).containsAll(this.topics));
            logger.debug("KafkaConsumer topics {} assigned", this.topics);

            logger.trace("KafkaConsumer topics {} poll starting", this.topics);
            poll(Duration.ofMillis(50));
            this.executor.execute(this::launch);
            logger.debug("KafkaConsumer topics {} poll started.", this.topics);
        }

        private void launch() {
            logger.info("KafkaConsumer '{}' started consuming events from topics: {}", clientId, topics);
            while (isActive.get()) {
                try {
                    poll(POLL_TIMEOUT);
                } catch (WakeupException | InterruptException ignore) {
                    // do nothing
                } catch (Exception e) {
                    logger.error("KafkaConsumer '{}' for {} topics got unhandled exception", clientId, topics, e);
                    consumer.close(Duration.ofMinutes(5));
                    throw e;
                }
            }
        }

        private void poll(Duration maxPollTimeout) {
            var records = consumer.poll(maxPollTimeout);
            if (!records.isEmpty()) {
                logger.info("KafkaConsumer '{}' polled '{}' records from topics: {}", clientId, records.count(), topics);
            } else {
                logger.trace("KafkaConsumer '{}' polled '{}' records from topics {}...", clientId, records.count(), topics);
            }

            for (var record : records) {
                messageQueue.offer(record);
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

            final List<ConsumerRecord<byte[], byte[]>> drainTo = new ArrayList<>();
            messageQueue.drainTo(drainTo, expectedEvents);
            for (var consumerRecord : drainTo) {
                var event = new ReceivedEventImpl(consumerRecord);
                receivedEvents.add(event);
            }

            if (receivedEvents.size() == expectedEvents) {
                receivedPreviously.addAll(receivedEvents);
                return List.copyOf(receivedEvents);
            }

            try {
                Awaitility.await()
                        .atMost(timeout)
                        .until(() -> {
                            try {
                                var received = messageQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
                                if (received == null) {
                                    return receivedEvents;
                                }

                                var event = new ReceivedEventImpl(received);
                                receivedEvents.add(event);
                                return receivedEvents;
                            } catch (InterruptedException e) {
                                return Assertions
                                        .fail(String.format("Expected to receive at least %s event, but was interrupted: %s",
                                                expectedEvents, e.getMessage()));
                            }
                        }, received -> received.size() >= expectedEvents);
            } catch (ConditionTimeoutException e) {
                // do nothing
            }

            receivedPreviously.addAll(receivedEvents);
            return List.copyOf(receivedEvents);
        }

        private List<ReceivedEvent> getReceivedEqualsInTime(int expected, @NotNull Duration timeToWait) {
            try {
                Thread.sleep(timeToWait.toMillis());

                final List<ReceivedEvent> receivedEvents = new ArrayList<>();
                final List<ConsumerRecord<byte[], byte[]>> drainTo = new ArrayList<>();
                messageQueue.drainTo(drainTo);
                for (var consumerRecord : drainTo) {
                    var event = new ReceivedEventImpl(consumerRecord);
                    receivedEvents.add(event);
                }

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

        @Override
        public void close() throws Exception {
            stop();
        }

        void stop() {
            if (isActive.compareAndSet(true, false)) {
                logger.debug("Stopping KafkaConsumer '{}' for {} topics...", clientId, topics);
                final long started = System.nanoTime();

                try {
                    executor.shutdownNow();
                    consumer.wakeup();
                    executor.awaitTermination(1, TimeUnit.MINUTES);
                    consumer.close(Duration.ofMinutes(5));
                } catch (Exception e) {
                    // do nothing
                } finally {
                    reset();
                    logger.info("Stopped KafkaConsumer '{}' for {} topics took {}", clientId, topics,
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
    public @NotNull KafkaConnection withProperties(@NotNull Properties properties) {
        final Properties kafkaProperties = new Properties();
        kafkaProperties.putAll(params.properties());
        kafkaProperties.putAll(properties);

        final Properties networkProperties = paramsInNetwork().map(props -> {
            final Properties kafkaNetworkProperties = new Properties();
            kafkaNetworkProperties.putAll(props.properties());
            kafkaNetworkProperties.putAll(properties);
            return kafkaNetworkProperties;
        }).orElse(null);

        return new KafkaConnectionClosableImpl(kafkaProperties, networkProperties);
    }

    @Override
    public void send(@NotNull String topic, @NotNull Event... events) {
        send(topic, Arrays.asList(events));
    }

    @Override
    public void send(@NotNull String topic, @NotNull List<Event> events) {
        if (isClosed) {
            throw new KafkaConnectionException("Can't send cause was closed");
        }

        if (this.producer == null) {
            try {
                this.producer = getProducer(params.properties());
            } catch (Exception e) {
                throw new KafkaConnectionException("Can't create KafkaProducer", e);
            }
        }

        createTopicsIfNeeded(Set.of(topic), false);

        for (Event event : events) {
            final byte[] key = (event.key() == null)
                    ? null
                    : event.key().asBytes();

            final List<Header> headers = (event.headers().isEmpty())
                    ? null
                    : event.headers().stream()
                            .map(header -> new RecordHeader(header.key(), header.value().asBytes()))
                            .collect(Collectors.toList());

            try {
                logger.trace("KafkaProducer sending event: {}", event);
                var result = producer.send(new ProducerRecord<>(topic, null, key, event.value().asBytes(), headers))
                        .get(10, TimeUnit.SECONDS);
                logger.info(
                        "KafkaProducer sent event to topic '{}' with offset '{}' with partition '{}' with timestamp '{}' event: {}",
                        topic, result.offset(), result.partition(), result.timestamp(), event);
            } catch (Exception e) {
                throw new KafkaConnectionException("KafkaProducer sent event failed: " + event, e);
            }
        }
    }

    @Override
    public @NotNull Consumer subscribe(@NotNull String... topics) {
        return subscribe(new HashSet<>(Arrays.asList(topics)));
    }

    @Override
    public @NotNull Consumer subscribe(@NotNull Set<String> topics) {
        if (isClosed) {
            throw new KafkaConnectionException("Can't subscribed cause was closed");
        }

        try {
            var admin = getAdmin();
            createTopicsIfNeeded(admin, topics, false);

            var topicInfo = Awaitility.await()
                    .atMost(Duration.ofMinutes(1))
                    .pollInterval(Duration.ofMillis(100))
                    .until(() -> {
                        try {
                            return admin.describeTopics(topics).allTopicNames().get(10, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            logger.warn(e.getMessage());
                            return Collections.<String, TopicDescription>emptyMap();
                        }
                    }, result -> result.values().stream().map(TopicDescription::name).collect(Collectors.toSet())
                            .containsAll(topics));

            var topicPartition = topicInfo.entrySet().stream()
                    .filter(e -> topics.contains(e.getValue().name()))
                    .flatMap(e -> e.getValue().partitions().stream()
                            .map(p -> new TopicPartition(e.getValue().name(), p.partition())))
                    .collect(Collectors.toSet());

            final String id = UUID.randomUUID().toString().substring(0, 8);
            var kafkaConsumer = getConsumer(id, params.properties());
            var consumer = new ConsumerImpl(kafkaConsumer, id, topicPartition);
            consumers.add(consumer);
            return consumer;
        } catch (Exception e) {
            throw new KafkaConnectionException("Can't create KafkaConsumer", e);
        }
    }

    private static KafkaProducer<byte[], byte[]> getProducer(Properties properties) {
        final Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProperties.put(ProducerConfig.RETRIES_CONFIG, "3");
        producerProperties.putAll(properties);
        return new KafkaProducer<>(producerProperties, new ByteArraySerializer(), new ByteArraySerializer());
    }

    private static KafkaConsumer<byte[], byte[]> getConsumer(String consumerId, Properties properties) {
        final Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProperties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "5");
        consumerProperties.put(ConsumerConfig.CLIENT_ID_CONFIG, consumerId);
        consumerProperties.putAll(properties);
        return new KafkaConsumer<>(consumerProperties, new ByteArrayDeserializer(), new ByteArrayDeserializer());
    }

    private static Admin getAdmin(Properties properties) {
        final Properties adminProperties = new Properties();
        adminProperties.putAll(properties);
        return Admin.create(adminProperties);
    }

    private Admin getAdmin() {
        if (this.admin == null) {
            this.admin = getAdmin(params().properties());
        }

        return this.admin;
    }

    void createTopicsIfNeeded(@NotNull Set<String> topics, boolean reset) {
        createTopicsIfNeeded(getAdmin(), topics, reset);
    }

    static void createTopicsIfNeeded(@NotNull Admin admin, @NotNull Set<String> topics, boolean reset) {
        try {
            logger.trace("Looking for existing topics...");
            var existingTopics = admin.listTopics().names().get(1, TimeUnit.MINUTES);
            logger.debug("Found existing topics: {}", existingTopics);

            var topicsToCreate = topics.stream()
                    .filter(topic -> !existingTopics.contains(topic))
                    .map(topic -> new NewTopic(topic, Optional.of(1), Optional.empty()))
                    .collect(Collectors.toSet());

            var topicsToReset = existingTopics.stream()
                    .filter(topics::contains)
                    .collect(Collectors.toSet());

            if (!topicsToCreate.isEmpty()) {
                logger.trace("Topics {} creating...", topics);
                var result = admin.createTopics(topicsToCreate);
                result.all().get(1, TimeUnit.MINUTES);
                logger.info("Topics {} created", topics);
            } else if (reset && !topicsToReset.isEmpty()) {
                logger.trace("Topics {} already exist, but require reset, resetting...", topicsToReset);
                admin.deleteTopics(topicsToReset).all().get(1, TimeUnit.MINUTES);
                logger.debug("Topics {} reset success", topicsToReset);

                var topicsToCreateAfterReset = topicsToReset.stream()
                        .map(topic -> new NewTopic(topic, Optional.of(1), Optional.empty()))
                        .collect(Collectors.toSet());

                logger.trace("Topics {} reset status check...", topicsToReset);
                Awaitility.await()
                        .atMost(Duration.ofSeconds(35))
                        .pollInterval(Duration.ofMillis(100))
                        .until(() -> {
                            try {
                                return admin.describeTopics(topics).allTopicNames().get(10, TimeUnit.SECONDS);
                            } catch (Exception e) {
                                logger.warn(e.getMessage());
                                return Collections.<String, TopicDescription>emptyMap();
                            }
                        }, result -> result.values().stream()
                                .map(TopicDescription::name)
                                .filter(topicsToReset::contains)
                                .findFirst()
                                .isEmpty());
                logger.debug("Topics {} reset status check success", topicsToReset);

                logger.trace("Topics {} recreating...", topicsToReset);
                Awaitility.await().atMost(Duration.ofSeconds(35))
                        .pollInterval(Duration.ofMillis(50))
                        .until(() -> {
                            try {
                                admin.createTopics(topicsToCreateAfterReset).all().get(10, TimeUnit.SECONDS);
                                return true;
                            } catch (ExecutionException e) {
                                if (e.getCause() instanceof TopicExistsException) {
                                    Thread.sleep(500);
                                    return false;
                                } else {
                                    throw new KafkaConnectionException("Kafka Admin operation failed for topics: " + topics, e);
                                }
                            } catch (Exception e) {
                                throw new KafkaConnectionException("Kafka Admin operation failed for topics: " + topics, e);
                            }
                        });

                logger.info("Topics {} recreated", topicsToReset);
            } else {
                logger.debug("Topics already exist: {}", topics);
            }
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TopicExistsException) {
                logger.trace("Topics already exist exception received: {}", topics);
            } else {
                throw new KafkaConnectionException("Kafka Admin operation failed for topics: " + topics, e);
            }
        } catch (TopicExistsException e) {
            logger.trace("Topics already exist exception received: {}", topics);
        } catch (Exception e) {
            throw new KafkaConnectionException("Kafka Admin operation failed for topics: " + topics, e);
        }
    }

    @Override
    public @NotNull Admin admin() {
        final Properties adminProperties = new Properties();
        adminProperties.putAll(params.properties());
        return Admin.create(adminProperties);
    }

    @Override
    public void createTopics(@NotNull Set<String> topics) {
        try (var admin = admin()) {
            createTopicsIfNeeded(admin, topics, false);
        }
    }

    @Override
    public void dropTopics(@NotNull Set<String> topics) {
        try (var admin = admin()) {
            logger.trace("Looking for existing topics...");
            var existingTopics = admin.listTopics().names().get(1, TimeUnit.MINUTES);
            logger.debug("Found existing topics: {}", existingTopics);

            var topicsToDrop = existingTopics.stream()
                    .filter(topics::contains)
                    .collect(Collectors.toSet());

            if (!topicsToDrop.isEmpty()) {
                logger.trace("Topics {} dropping...", topicsToDrop);
                var deleteTopicsResult = admin.deleteTopics(topics);
                var deleteFutures = deleteTopicsResult.topicNameValues().values().toArray(KafkaFuture[]::new);
                KafkaFuture.allOf(deleteFutures).get(1, TimeUnit.MINUTES);
                logger.info("Required topics {} dropped", topicsToDrop);
            } else {
                logger.debug("Required topics already dropped: {}", topics);
            }
        } catch (Exception e) {
            throw new KafkaConnectionException("Kafka Admin operation failed for topics: " + topics, e);
        }
    }

    void clear() {
        for (var consumer : consumers) {
            try {
                consumer.stop();
            } catch (Exception e) {
                // do nothing
            }
        }
        consumers.clear();
    }

    void stop() {
        if (!isClosed) {
            isClosed = true;

            clear();

            if (admin != null) {
                try {
                    admin.close(Duration.ofMinutes(1));
                    admin = null;
                } catch (Exception e) {
                    // do nothing
                }
            }

            if (producer != null) {
                try {
                    producer.close(Duration.ofMinutes(1));
                    producer = null;
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        // do nothing
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        KafkaConnectionImpl that = (KafkaConnectionImpl) o;
        return Objects.equals(params, that.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(params);
    }

    @Override
    public String toString() {
        return params.bootstrapServers();
    }
}
