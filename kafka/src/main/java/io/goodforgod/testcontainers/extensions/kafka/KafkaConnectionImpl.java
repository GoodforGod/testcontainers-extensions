package io.goodforgod.testcontainers.extensions.kafka;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.awaitility.Awaitility;

@Internal
final class KafkaConnectionImpl implements KafkaConnection, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConnection.class);

    private volatile boolean isClosed = false;
    private volatile KafkaProducer<byte[], byte[]> producer;

    private final List<ConsumerImpl> consumers = new CopyOnWriteArrayList<>();
    private final Properties properties;

    KafkaConnectionImpl(Properties properties) {
        this.properties = properties;
    }

    static final class ConsumerImpl implements Consumer, AutoCloseable {

        private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final List<ReceivedEvent> receivedPreviously = new CopyOnWriteArrayList<>();
        private final AtomicBoolean isActive = new AtomicBoolean(true);

        private final KafkaConsumer<byte[], byte[]> consumer;
        private final BlockingQueue<ConsumerRecord<byte[], byte[]>> messageQueue = new ArrayBlockingQueue<>(100_000_000);
        private final List<String> topics;

        ConsumerImpl(KafkaConsumer<byte[], byte[]> consumer, List<String> topics) {
            this.consumer = consumer;
            this.topics = topics;
            executor.execute(this::launch);
        }

        private void launch() {
            while (isActive.get()) {
                try {
                    var records = consumer.poll(Duration.ofSeconds(30));
                    for (var record : records) {
                        messageQueue.offer(record);
                    }
                } catch (WakeupException ignore) {} catch (Exception e) {
                    logger.error("Kafka Consumer for {} topics got unhandled exception", topics, e);
                    consumer.close();
                    break;
                }
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
        public @NotNull List<ReceivedEvent> getReceivedAtLeast(int expectedAtLeast, @NotNull Duration timeout) {
            final List<ReceivedEvent> receivedEvents = new CopyOnWriteArrayList<>();

            final List<ConsumerRecord<byte[], byte[]>> drainTo = new ArrayList<>();
            messageQueue.drainTo(drainTo, expectedAtLeast);
            for (var consumerRecord : drainTo) {
                var event = new ReceivedEventImpl(consumerRecord);
                receivedEvents.add(event);
            }

            if (receivedEvents.size() == expectedAtLeast) {
                receivedPreviously.addAll(receivedEvents);
                return receivedEvents;
            }

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
                            return Assertions.fail(String.format("Expected to receive at least %s event, but was interrupted: %s",
                                    expectedAtLeast, e.getMessage()));
                        }
                    }, received -> received.size() >= expectedAtLeast);

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
                Assertions.fail("Expected to receive 0 events, but receive at least 1 event");
            }
        }

        @Override
        public @NotNull ReceivedEvent assertReceived(@NotNull Duration timeout) {
            var received = getReceived(timeout);
            return received.orElseGet(() -> Assertions.fail("Expected to receive 1 event, but receive 0 event"));
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
        public boolean checkReceived(@NotNull Duration timeout) {
            var received = getReceived(timeout);
            return received.isPresent();
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
            if (isActive.compareAndSet(true, false)) {
                logger.debug("Stopping Kafka Consumer for {} topics...", topics);
                final long started = System.nanoTime();

                consumer.wakeup();
                executor.shutdownNow();
                consumer.close(Duration.ofMinutes(3));

                receivedPreviously.clear();
                messageQueue.clear();

                logger.info("Stopped Kafka Consumer for {} topics took {}", topics,
                        Duration.ofNanos(System.nanoTime() - started));
            }
        }
    }

    @Override
    public void send(@NotNull String topic, @NotNull Event... events) {
        send(topic, Arrays.asList(events));
    }

    @Override
    public void send(@NotNull String topic, @NotNull List<Event> events) {
        if (isClosed) {
            throw new IllegalStateException("Can't subscribed cause was closed");
        }

        if (this.producer == null) {
            this.producer = getProducer(properties);
        }

        for (Event event : events) {
            this.producer.send(new ProducerRecord<>(topic, event.key().asBytes(), event.value().asBytes()));
        }
    }

    @Override
    public @NotNull Consumer subscribe(@NotNull String... topics) {
        return subscribe(Arrays.asList(topics));
    }

    @Override
    public @NotNull Consumer subscribe(@NotNull List<String> topics) {
        if (isClosed) {
            throw new IllegalStateException("Can't subscribed cause was closed");
        }

        var kafkaConsumer = getConsumer(properties);
        var consumer = new ConsumerImpl(kafkaConsumer, topics);
        consumers.add(consumer);
        return consumer;
    }

    private static KafkaProducer<byte[], byte[]> getProducer(Properties properties) {
        final Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProperties.put(ProducerConfig.RETRIES_CONFIG, "3");
        producerProperties.putAll(properties);
        return new KafkaProducer<>(producerProperties, new ByteArraySerializer(), new ByteArraySerializer());
    }

    private static KafkaConsumer<byte[], byte[]> getConsumer(Properties properties) {
        final Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        consumerProperties.putAll(properties);
        return new KafkaConsumer<>(consumerProperties, new ByteArrayDeserializer(), new ByteArrayDeserializer());
    }

    @Override
    public void close() throws Exception {
        if (!isClosed) {
            isClosed = true;
            if (producer != null) {
                producer.close(Duration.ofMinutes(3));
            }

            for (var consumer : consumers) {
                consumer.close();
            }
            consumers.clear();
        }
    }
}
