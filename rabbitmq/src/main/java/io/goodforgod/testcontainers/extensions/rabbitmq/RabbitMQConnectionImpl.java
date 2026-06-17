package io.goodforgod.testcontainers.extensions.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;

@Internal
class RabbitMQConnectionImpl implements RabbitMQConnection {

    static final String PROP_URI = "uri";
    static final String PROP_HOST = "host";
    static final String PROP_PORT = "port";
    static final String PROP_USERNAME = "username";
    static final String PROP_PASSWORD = "password";
    static final String PROP_VIRTUAL_HOST = "virtual.host";

    static final int RABBITMQ_PORT = 5672;

    private static final class ParamsImpl implements Params {

        private final String uri;
        private final String host;
        private final int port;
        private final String username;
        private final String password;
        private final String virtualHost;
        private final Properties properties;

        private ParamsImpl(Properties properties) {
            final String host = properties.getProperty(PROP_HOST, "localhost");
            final int port = Integer.parseInt(properties.getProperty(PROP_PORT, String.valueOf(RABBITMQ_PORT)));
            final String username = properties.getProperty(PROP_USERNAME, "guest");
            final String password = properties.getProperty(PROP_PASSWORD, "guest");
            final String virtualHost = properties.getProperty(PROP_VIRTUAL_HOST, "/");
            this.uri = properties.getProperty(PROP_URI, buildUri(host, port, username, password, virtualHost));
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.virtualHost = virtualHost;
            this.properties = properties;
        }

        @Override
        public @NotNull String uri() {
            return uri;
        }

        @Override
        public @NotNull String host() {
            return host;
        }

        @Override
        public int port() {
            return port;
        }

        @Override
        public @NotNull String username() {
            return username;
        }

        @Override
        public @NotNull String password() {
            return password;
        }

        @Override
        public @NotNull String virtualHost() {
            return virtualHost;
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
            return port == params.port && Objects.equals(uri, params.uri) && Objects.equals(host, params.host)
                    && Objects.equals(username, params.username) && Objects.equals(password, params.password)
                    && Objects.equals(virtualHost, params.virtualHost) && Objects.equals(properties, params.properties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri, host, port, username, password, virtualHost, properties);
        }

        @Override
        public String toString() {
            return uri;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConnection.class);

    private volatile boolean isClosed = false;
    private volatile Connection connection;

    private final Map<String, ConsumerImpl> consumerByQueue = new ConcurrentHashMap<>();
    private final ParamsImpl params;
    @Nullable
    private final ParamsImpl paramsInNetwork;

    RabbitMQConnectionImpl(Properties properties, @Nullable Properties propertiesInNetwork) {
        this.params = new ParamsImpl(properties);
        this.paramsInNetwork = (propertiesInNetwork == null)
                ? null
                : new ParamsImpl(propertiesInNetwork);
        this.connection = getConnection(properties);
    }

    static final class ConsumerImpl implements Consumer {

        private final List<ReceivedEvent> receivedPreviously = new CopyOnWriteArrayList<>();
        private final BlockingQueue<Delivery> messageQueue = new LinkedBlockingDeque<>();
        private final AtomicBoolean isActive = new AtomicBoolean(true);

        private final Channel channel;
        private final String queue;
        private final String consumerTag;

        ConsumerImpl(Channel channel, String queue, String consumerTag) {
            this.channel = channel;
            this.queue = queue;
            this.consumerTag = consumerTag;
        }

        @Override
        public void reset() {
            receivedPreviously.clear();
            messageQueue.clear();
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

                var event = new ReceivedEventImpl(queue, received);
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

            final List<Delivery> drainTo = new ArrayList<>();
            messageQueue.drainTo(drainTo, expectedEvents);
            for (var delivery : drainTo) {
                receivedEvents.add(new ReceivedEventImpl(queue, delivery));
            }

            if (receivedEvents.size() == expectedEvents) {
                receivedPreviously.addAll(receivedEvents);
                return List.copyOf(receivedEvents);
            }

            try {
                Awaitility.await()
                        .atMost(timeout)
                        .pollDelay(Duration.ofMillis(5))
                        .until(() -> {
                            for (int i = receivedEvents.size(); i < expectedEvents; i++) {
                                try {
                                    var received = messageQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
                                    if (received == null) {
                                        return receivedEvents;
                                    }

                                    receivedEvents.add(new ReceivedEventImpl(queue, received));
                                } catch (InterruptedException e) {
                                    // do nothing
                                }
                            }
                            return receivedEvents;
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
                final List<Delivery> drainTo = new ArrayList<>();
                messageQueue.drainTo(drainTo);
                for (var delivery : drainTo) {
                    receivedEvents.add(new ReceivedEventImpl(queue, delivery));
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
                    receivedPreviously.add(new ReceivedEventImpl(queue, received));
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

        boolean isClosed() {
            return !isActive.get();
        }

        @Override
        public void close() {
            stop();
        }

        void offer(Delivery delivery) {
            if (isActive.get()) {
                messageQueue.offer(delivery);
            }
        }

        void stop() {
            if (isActive.compareAndSet(true, false)) {
                try {
                    channel.basicCancel(consumerTag);
                } catch (Exception e) {
                    // do nothing
                }

                try {
                    channel.close();
                } catch (Exception e) {
                    // do nothing
                }

                reset();
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
    public @NotNull RabbitMQConnection withProperties(@NotNull Properties properties) {
        final Properties combinedProperties = new Properties();
        combinedProperties.putAll(params.properties());
        combinedProperties.putAll(properties);

        final Properties networkProperties = paramsInNetwork().map(props -> {
            final Properties rabbitNetworkProperties = new Properties();
            rabbitNetworkProperties.putAll(props.properties());
            rabbitNetworkProperties.putAll(properties);
            return rabbitNetworkProperties;
        }).orElse(null);

        return new RabbitMQConnectionClosableImpl(combinedProperties, networkProperties);
    }

    @Override
    public void send(@NotNull String exchange, @NotNull String routingKey, @NotNull List<Event> events) {
        if (isClosed) {
            throw new RabbitMQConnectionException("Can't send cause was closed");
        }

        try (Channel channel = connection.createChannel()) {
            if (exchange.isBlank()) {
                ensureQueueExists(channel, routingKey);
            }

            for (Event event : events) {
                final Map<String, Object> headers = new LinkedHashMap<>();
                for (var header : event.headers()) {
                    headers.put(header.key(), header.value().asString());
                }

                final AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                        .messageId(UUID.randomUUID().toString())
                        .headers(headers.isEmpty()
                                ? null
                                : headers)
                        .build();

                logger.trace("RabbitMQ publishing event: {}", event);
                channel.basicPublish(exchange, routingKey, properties, event.value().asBytes());
                logger.info("RabbitMQ published event to exchange '{}' and routing key '{}': {}", exchange, routingKey, event);
            }
        } catch (Exception e) {
            throw new RabbitMQConnectionException("RabbitMQ publish failed", e);
        }
    }

    @Override
    public @NotNull Consumer subscribe(@NotNull String queue) {
        if (isClosed) {
            throw new RabbitMQConnectionException("Can't subscribe cause was closed");
        }

        try {
            final ConsumerImpl consumer = consumerByQueue.computeIfAbsent(queue, this::createConsumer);
            if (consumer.isClosed()) {
                final ConsumerImpl activeConsumer = createConsumer(queue);
                consumerByQueue.put(queue, activeConsumer);
                return activeConsumer;
            }
            return consumer;
        } catch (Exception e) {
            throw new RabbitMQConnectionException("Can't create RabbitMQ consumer", e);
        }
    }

    private ConsumerImpl createConsumer(String queue) {
        try {
            final Channel channel = connection.createChannel();
            ensureQueueExists(channel, queue);
            final ConsumerImpl[] holder = new ConsumerImpl[1];
            final String consumerTag = channel.basicConsume(queue, true,
                    (tag, delivery) -> holder[0].offer(delivery),
                    tag -> logger.debug("RabbitMQ consumer '{}' for queue '{}' cancelled", tag, queue));
            holder[0] = new ConsumerImpl(channel, queue, consumerTag);
            return holder[0];
        } catch (Exception e) {
            throw new RabbitMQConnectionException("Can't create RabbitMQ consumer for queue: " + queue, e);
        }
    }

    @Override
    public void declareQueues(@NotNull Set<QueueSpec> queues) {
        if (queues.isEmpty()) {
            return;
        }

        try (Channel channel = connection.createChannel()) {
            for (var queue : queues) {
                channel.queueDeclare(queue.name(), queue.durable(), queue.exclusive(), queue.autoDelete(), queue.arguments());
            }
        } catch (Exception e) {
            throw new RabbitMQConnectionException("RabbitMQ queue declaration failed", e);
        }
    }

    @Override
    public void declareExchanges(@NotNull Set<ExchangeSpec> exchanges) {
        if (exchanges.isEmpty()) {
            return;
        }

        try (Channel channel = connection.createChannel()) {
            for (var exchange : exchanges) {
                channel.exchangeDeclare(exchange.name(), exchange.type(), exchange.durable(), exchange.autoDelete(),
                        exchange.arguments());
            }
        } catch (Exception e) {
            throw new RabbitMQConnectionException("RabbitMQ exchange declaration failed", e);
        }
    }

    @Override
    public void declareBindings(@NotNull Set<BindingSpec> bindings) {
        if (bindings.isEmpty()) {
            return;
        }

        try (Channel channel = connection.createChannel()) {
            for (var binding : bindings) {
                ensureQueueExists(channel, binding.queue());
                channel.queueBind(binding.queue(), binding.exchange(), binding.routingKey(), binding.arguments());
            }
        } catch (Exception e) {
            throw new RabbitMQConnectionException("RabbitMQ binding declaration failed", e);
        }
    }

    @Override
    public void deleteQueues(@NotNull Set<String> queues) {
        if (queues.isEmpty()) {
            return;
        }

        try (Channel channel = connection.createChannel()) {
            for (var queue : queues) {
                try {
                    channel.queueDelete(queue);
                } catch (Exception e) {
                    logger.debug("RabbitMQ queue '{}' delete skipped: {}", queue, e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RabbitMQConnectionException("RabbitMQ queue deletion failed", e);
        }
    }

    @Override
    public void deleteExchanges(@NotNull Set<String> exchanges) {
        if (exchanges.isEmpty()) {
            return;
        }

        try (Channel channel = connection.createChannel()) {
            for (var exchange : exchanges) {
                if (exchange.isBlank()) {
                    continue;
                }

                try {
                    channel.exchangeDelete(exchange);
                } catch (Exception e) {
                    logger.debug("RabbitMQ exchange '{}' delete skipped: {}", exchange, e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RabbitMQConnectionException("RabbitMQ exchange deletion failed", e);
        }
    }

    private static void ensureQueueExists(Channel channel, String queue) throws Exception {
        try {
            channel.queueDeclarePassive(queue);
        } catch (Exception e) {
            channel.queueDeclare(queue, false, false, false, Collections.emptyMap());
        }
    }

    private static Connection getConnection(Properties properties) {
        try {
            final ConnectionFactory factory = new ConnectionFactory();
            final String uri = properties.getProperty(PROP_URI);
            if (uri != null && !uri.isBlank()) {
                factory.setUri(uri);
            } else {
                factory.setHost(properties.getProperty(PROP_HOST, "localhost"));
                factory.setPort(Integer.parseInt(properties.getProperty(PROP_PORT, String.valueOf(RABBITMQ_PORT))));
                factory.setUsername(properties.getProperty(PROP_USERNAME, "guest"));
                factory.setPassword(properties.getProperty(PROP_PASSWORD, "guest"));
                factory.setVirtualHost(properties.getProperty(PROP_VIRTUAL_HOST, "/"));
            }

            if (properties.containsKey("automatic.recovery.enabled")) {
                factory.setAutomaticRecoveryEnabled(Boolean.parseBoolean(properties.getProperty("automatic.recovery.enabled")));
            }
            if (properties.containsKey("topology.recovery.enabled")) {
                factory.setTopologyRecoveryEnabled(Boolean.parseBoolean(properties.getProperty("topology.recovery.enabled")));
            }
            if (properties.containsKey("requested.heartbeat")) {
                factory.setRequestedHeartbeat(Integer.parseInt(properties.getProperty("requested.heartbeat")));
            }
            if (properties.containsKey("connection.timeout")) {
                factory.setConnectionTimeout(Integer.parseInt(properties.getProperty("connection.timeout")));
            }
            if (properties.containsKey("handshake.timeout")) {
                factory.setHandshakeTimeout(Integer.parseInt(properties.getProperty("handshake.timeout")));
            }
            if (properties.containsKey("channel.rpc.timeout")) {
                factory.setChannelRpcTimeout(Integer.parseInt(properties.getProperty("channel.rpc.timeout")));
            }

            return factory.newConnection("testcontainers-extensions-rabbitmq");
        } catch (Exception e) {
            throw new RabbitMQConnectionException("Can't create RabbitMQ connection", e);
        }
    }

    static String buildUri(String host, int port, String username, String password, String virtualHost) {
        final String vHost = (virtualHost == null || virtualHost.isBlank())
                ? "/"
                : virtualHost;
        return String.format("amqp://%s:%s@%s:%s/%s",
                URLEncoder.encode(username, StandardCharsets.UTF_8),
                URLEncoder.encode(password, StandardCharsets.UTF_8),
                host,
                port,
                URLEncoder.encode(vHost, StandardCharsets.UTF_8));
    }

    void clear() {
        for (var consumer : consumerByQueue.values()) {
            try {
                consumer.stop();
            } catch (Exception e) {
                // do nothing
            }
        }
        consumerByQueue.clear();
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
        RabbitMQConnectionImpl that = (RabbitMQConnectionImpl) o;
        return Objects.equals(params, that.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(params);
    }

    @Override
    public String toString() {
        return params.uri();
    }
}
