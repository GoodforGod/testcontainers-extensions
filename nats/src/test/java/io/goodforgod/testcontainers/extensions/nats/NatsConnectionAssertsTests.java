package io.goodforgod.testcontainers.extensions.nats;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.time.Duration;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

@TestcontainersNats(mode = ContainerMode.PER_CLASS)
class NatsConnectionAssertsTests {

    @ConnectionNats
    private NatsConnection connection;

    @Test
    void getReceived() {
        // given
        var topic = "example";
        try (var subscriber = connection.subscribe(topic)) {
            // when
            var event = Event.builder()
                    .withValue(new JSONObject().put("name", "bob"))
                    .withHeader("1", "1")
                    .withHeader("2", "2")
                    .build();
            connection.send(topic, event);

            // then
            var received = subscriber.getReceived(Duration.ofMillis(100));
            assertTrue(received.isPresent());
            assertEquals(topic, received.get().subject());
            assertEquals(event.value(), received.get().value());
            assertEquals(event.value().toString(), received.get().value().toString());
            assertEquals(event.value().asString(), received.get().value().asString());
            assertEquals(event.value().asJson().toString(), received.get().value().asJson().toString());
            assertEquals(2, event.headers().size());
            assertEquals(event.headers(), received.get().headers());
            assertEquals(event.headers().get(0), received.get().headers().get(0));
            assertEquals(event.headers().get(0).toString(), received.get().headers().get(0).toString());
            assertNotNull(received.get().toString());
        }
    }

    @Test
    void getReceivedAtLeast() {
        var topic = "example";
        try (var subscriber = connection.subscribe(topic)) {
            connection.send(topic, Event.ofValue("value1"), Event.ofValue("value2"));
            var received = subscriber.getReceivedAtLeast(2, Duration.ofMillis(100));
            assertEquals(2, received.size());
            assertNotEquals(received.get(0), received.get(1));
            assertNotEquals(received.get(0).toString(), received.get(1).toString());
        }
    }

    @Test
    void assertReceivedNone() {
        var topic = "example";
        try (var subscriber = connection.subscribe(topic)) {
            subscriber.assertReceivedNone(Duration.ofMillis(100));
        }
    }

    @Test
    void assertReceivedNoneThrows() {
        var topic = "example";
        try (var subscriber = connection.subscribe(topic)) {
            connection.send(topic, Event.ofValue("value"));
            assertThrows(AssertionFailedError.class, () -> subscriber.assertReceivedNone(Duration.ofMillis(100)));
        }
    }

    @Test
    void assertReceived() {
        var topic = "example";
        try (var subscriber = connection.subscribe(topic)) {
            connection.send(topic, Event.builder().withValue("value").withHeader("1", "1").build());
            var receivedEvent = subscriber.assertReceivedAtLeast(1, Duration.ofMillis(100));
            assertNotNull(receivedEvent.toString());
        }
    }

    @Test
    void assertReceivedThrows() {
        var topic = "example";
        try (var subscriber = connection.subscribe(topic)) {
            assertThrows(AssertionFailedError.class, () -> subscriber.assertReceivedAtLeast(1, Duration.ofMillis(100)));
        }
    }

    @Test
    void assertReceivedAtLeast() {
        var topic = "example";
        try (var subscriber = connection.subscribe(topic)) {
            connection.send(topic, Event.ofValue("value1"), Event.ofValue("value2"));
            subscriber.assertReceivedAtLeast(2, Duration.ofMillis(100));
        }
    }

    @Test
    void assertReceivedAtLeastThrows() {
        var topic = "example";
        try (var subscriber = connection.subscribe(topic)) {
            assertThrows(AssertionFailedError.class, () -> subscriber.assertReceivedAtLeast(2, Duration.ofMillis(100)));
        }
    }

    @Test
    void assertReceivedEquals() {
        var topic = "example";
        try (var subscriber = connection.subscribe(topic)) {
            connection.send(topic, Event.builder().withValue("value1").build(), Event.ofValue("value2"));
            var receivedEvents = subscriber.assertReceivedEqualsInTime(2, Duration.ofMillis(100));
            assertNotEquals(receivedEvents.get(0), receivedEvents.get(1));
            assertNotEquals(receivedEvents.get(0).toString(), receivedEvents.get(1).toString());
        }
    }

    @Test
    void assertReceivedEqualsThrows() {
        var topic = "example";
        try (var subscriber = connection.subscribe(topic)) {
            assertThrows(AssertionFailedError.class, () -> subscriber.assertReceivedEqualsInTime(2, Duration.ofMillis(100)));
        }
    }

    @Test
    void checkReceivedNone() {
        var topic = "example";
        try (var subscriber = connection.subscribe(topic)) {
            assertTrue(subscriber.checkReceivedNone(Duration.ofMillis(100)));
        }
    }

    @Test
    void checkReceivedNoneThrows() {
        var topic = "example";
        try (var subscriber = connection.subscribe(topic)) {
            connection.send(topic, Event.ofValue("value"));
            assertFalse(subscriber.checkReceivedNone(Duration.ofMillis(100)));
        }
    }

    @Test
    void checkReceivedAtLeast() {
        var topic = "example";
        try (var subscriber = connection.subscribe(topic)) {
            connection.send(topic, Event.ofValue("value1"), Event.ofValue("value2"));
            assertTrue(subscriber.checkReceivedAtLeast(2, Duration.ofMillis(100)));
        }
    }

    @Test
    void checkReceivedAtLeastThrows() {
        var topic = "example";
        try (var subscriber = connection.subscribe(topic)) {
            assertFalse(subscriber.checkReceivedAtLeast(2, Duration.ofMillis(100)));
        }
    }

    @Test
    void checkReceivedEquals() {
        var topic = "example";
        try (var subscriber = connection.subscribe(topic)) {
            connection.send(topic, Event.ofValue("value1"), Event.ofValue("value2"));
            assertTrue(subscriber.checkReceivedEqualsInTime(2, Duration.ofMillis(100)));
        }
    }

    @Test
    void checkReceivedEqualsThrows() {
        var topic = "example";
        try (var subscriber = connection.subscribe(topic)) {
            assertFalse(subscriber.checkReceivedEqualsInTime(2, Duration.ofMillis(100)));
        }
    }
}
