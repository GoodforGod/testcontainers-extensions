package io.goodforgod.testcontainers.extensions.rabbitmq;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.time.Duration;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

@TestcontainersRabbitMQ(mode = ContainerMode.PER_CLASS,
        topology = @Topology(
                queues = {
                        @Queue(name = "example"),
                        @Queue(name = "routed")
                },
                exchanges = @Exchange(name = "events", type = Exchange.Type.DIRECT),
                bindings = @Binding(queue = "routed", exchange = "events", routingKey = "route")))
class RabbitMQConnectionAssertsTests {

    @ConnectionRabbitMQ
    private RabbitMQConnection connection;

    @Test
    void params() {
        assertNotNull(connection.params().uri());
        assertNotNull(connection.params().host());
        assertNotNull(connection.params().username());
        assertNotNull(connection.params().password());
        assertNotNull(connection.params().properties());
    }

    @Test
    void getReceived() {
        var queue = "example";
        try (var consumer = connection.subscribe(queue)) {
            var event = Event.builder()
                    .withValue(new JSONObject().put("name", "bob"))
                    .withHeader("1", "1")
                    .withHeader("2", "2")
                    .build();
            connection.send(queue, event);

            var received = consumer.getReceived(Duration.ofMillis(300));
            assertTrue(received.isPresent());
            assertEquals(queue, received.get().queue());
            assertEquals("", received.get().exchange());
            assertEquals(queue, received.get().routingKey());
            assertTrue(received.get().deliveryTag() > 0);
            assertEquals(event.value(), received.get().value());
            assertEquals(event.value().asString(), received.get().value().asString());
            assertEquals(event.value().asJson().toString(), received.get().value().asJson().toString());
            assertEquals(2, event.headers().size());
            assertEquals(event.headers(), received.get().headers());
            assertNotNull(received.get().delivery());
            assertNotNull(received.get().toString());
        }
    }

    @Test
    void exchangeRouting() {
        try (var consumer = connection.subscribe("routed")) {
            connection.send("events", "route", Event.ofValue("value1"), Event.ofValue("value2"));
            var received = consumer.assertReceivedAtLeast(2, Duration.ofMillis(500));
            assertEquals(2, received.size());
            assertEquals("events", received.get(0).exchange());
            assertEquals("route", received.get(0).routingKey());
        }
    }

    @Test
    void assertReceivedNone() {
        try (var consumer = connection.subscribe("example")) {
            consumer.assertReceivedNone(Duration.ofMillis(150));
        }
    }

    @Test
    void assertReceivedNoneThrows() {
        try (var consumer = connection.subscribe("example")) {
            connection.send("example", Event.ofValue("value"));
            assertThrows(AssertionFailedError.class, () -> consumer.assertReceivedNone(Duration.ofMillis(150)));
        }
    }

    @Test
    void assertReceived() {
        try (var consumer = connection.subscribe("example")) {
            connection.send("example", Event.builder().withValue("value").withHeader("1", "1").build());
            var received = consumer.assertReceivedAtLeast(1, Duration.ofMillis(300));
            assertNotNull(received.get(0).toString());
        }
    }

    @Test
    void assertReceivedThrows() {
        try (var consumer = connection.subscribe("example")) {
            assertThrows(AssertionFailedError.class, () -> consumer.assertReceivedAtLeast(1, Duration.ofMillis(150)));
        }
    }

    @Test
    void assertReceivedEquals() {
        try (var consumer = connection.subscribe("example")) {
            connection.send("example", Event.ofValue("value1"), Event.ofValue("value2"));
            var received = consumer.assertReceivedEqualsInTime(2, Duration.ofMillis(300));
            assertEquals(2, received.size());
            assertNotEquals(received.get(0), received.get(1));
        }
    }

    @Test
    void assertReceivedEqualsThrows() {
        try (var consumer = connection.subscribe("example")) {
            assertThrows(AssertionFailedError.class, () -> consumer.assertReceivedEqualsInTime(2, Duration.ofMillis(150)));
        }
    }

    @Test
    void checkReceivedNone() {
        try (var consumer = connection.subscribe("example")) {
            assertTrue(consumer.checkReceivedNone(Duration.ofMillis(150)));
        }
    }

    @Test
    void checkReceivedAtLeast() {
        try (var consumer = connection.subscribe("example")) {
            connection.send("example", Event.ofValue("value1"), Event.ofValue("value2"));
            assertTrue(consumer.checkReceivedAtLeast(2, Duration.ofMillis(300)));
        }
    }

    @Test
    void checkReceivedEquals() {
        try (var consumer = connection.subscribe("example")) {
            connection.send("example", Event.ofValue("value1"), Event.ofValue("value2"));
            assertTrue(consumer.checkReceivedEqualsInTime(2, Duration.ofMillis(300)));
        }
    }
}
