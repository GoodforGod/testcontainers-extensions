package io.goodforgod.testcontainers.extensions.kafka;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

@TestcontainersKafka(mode = ContainerMode.PER_CLASS, image = "confluentinc/cp-kafka:7.5.4")
class KafkaConnectionAssertsTests {

    @ConnectionKafka
    private KafkaConnection connection;

    @Test
    void admin() throws Exception {
        connection.createTopics(Set.of("test-topic"));
        final Set<String> createdTopics = connection.admin().listTopics().names().get(10, TimeUnit.SECONDS);
        assertTrue(createdTopics.contains("test-topic"));

        connection.dropTopics(Set.of("test-topic"));
        final Set<String> droppedTopics = connection.admin().listTopics().names().get(10, TimeUnit.SECONDS);
        assertFalse(droppedTopics.contains("test-topic"));
    }

    @Test
    void getReceived() {
        // given
        var topic = "example";
        try (var consumer = connection.subscribe(topic)) {
            // when
            var event = Event.builder()
                    .withKey("1")
                    .withValue(new JSONObject().put("name", "bob"))
                    .withHeader("1", "1")
                    .withHeader("2", "2")
                    .build();
            connection.send(topic, event);

            // then
            var received = consumer.getReceived(Duration.ofSeconds(1));
            assertTrue(received.isPresent());
            assertNotEquals(-1, received.get().offset());
            assertNotEquals(-1, received.get().partition());
            assertNotEquals(-1, received.get().timestamp());
            assertEquals(topic, received.get().topic());
            assertNotNull(received.get().datetime());
            assertEquals(event.key(), received.get().key());
            assertEquals(event.key().toString(), received.get().key().toString());
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
        try (var consumer = connection.subscribe(topic)) {
            connection.send(topic, Event.ofValue("value1"), Event.ofValue("value2"));
            var received = consumer.getReceivedAtLeast(2, Duration.ofSeconds(1));
            assertEquals(2, received.size());
            assertNotEquals(received.get(0), received.get(1));
            assertNotEquals(received.get(0).toString(), received.get(1).toString());
        }
    }

    @Test
    void assertReceivedNone() {
        var topic = "example";
        try (var consumer = connection.subscribe(topic)) {
            consumer.assertReceivedNone(Duration.ofSeconds(1));
        }
    }

    @Test
    void assertReceivedNoneThrows() {
        var topic = "example";
        try (var consumer = connection.subscribe(topic)) {
            connection.send(topic, Event.ofValue("value"));
            assertThrows(AssertionFailedError.class, () -> consumer.assertReceivedNone(Duration.ofSeconds(1)));
        }
    }

    @Test
    void assertReceived() {
        var topic = "example";
        try (var consumer = connection.subscribe(topic)) {
            connection.send(topic, Event.builder().withValue("value").withHeader("1", "1").build());
            var receivedEvent = consumer.assertReceivedAtLeast(1, Duration.ofSeconds(1));
            assertNotNull(receivedEvent.toString());
        }
    }

    @Test
    void assertReceivedThrows() {
        var topic = "example";
        try (var consumer = connection.subscribe(topic)) {
            assertThrows(AssertionFailedError.class, () -> consumer.assertReceivedAtLeast(1, Duration.ofSeconds(1)));
        }
    }

    @Test
    void assertReceivedAtLeast() {
        var topic = "example";
        try (var consumer = connection.subscribe(topic)) {
            connection.send(topic, Event.ofValue("value1"), Event.ofValue("value2"));
            consumer.assertReceivedAtLeast(2, Duration.ofSeconds(1));
        }
    }

    @Test
    void assertReceivedAtLeastThrows() {
        var topic = "example";
        try (var consumer = connection.subscribe(topic)) {
            assertThrows(AssertionFailedError.class, () -> consumer.assertReceivedAtLeast(2, Duration.ofSeconds(1)));
        }
    }

    @Test
    void assertReceivedEquals() {
        var topic = "example";
        try (var consumer = connection.subscribe(topic)) {
            connection.send(topic, Event.builder().withValue("value1").withKey("1").build(),
                    Event.ofValue("value2"));
            var receivedEvents = consumer.assertReceivedEqualsInTime(2, Duration.ofSeconds(1));
            assertNotEquals(receivedEvents.get(0), receivedEvents.get(1));
            assertNotEquals(receivedEvents.get(0).toString(), receivedEvents.get(1).toString());
        }
    }

    @Test
    void assertReceivedEqualsThrows() {
        var topic = "example";
        try (var consumer = connection.subscribe(topic)) {
            assertThrows(AssertionFailedError.class, () -> consumer.assertReceivedEqualsInTime(2, Duration.ofSeconds(1)));
        }
    }

    @Test
    void checkReceivedNone() {
        var topic = "example";
        try (var consumer = connection.subscribe(topic)) {
            assertTrue(consumer.checkReceivedNone(Duration.ofSeconds(1)));
        }
    }

    @Test
    void checkReceivedNoneThrows() {
        var topic = "example";
        try (var consumer = connection.subscribe(topic)) {
            connection.send(topic, Event.ofValue("value"));
            assertFalse(consumer.checkReceivedNone(Duration.ofSeconds(1)));
        }
    }

    @Test
    void checkReceivedAtLeast() {
        var topic = "example";
        try (var consumer = connection.subscribe(topic)) {
            connection.send(topic, Event.ofValue("value1"), Event.ofValue("value2"));
            assertTrue(consumer.checkReceivedAtLeast(2, Duration.ofSeconds(1)));
        }
    }

    @Test
    void checkReceivedAtLeastThrows() {
        var topic = "example";
        try (var consumer = connection.subscribe(topic)) {
            assertFalse(consumer.checkReceivedAtLeast(2, Duration.ofSeconds(1)));
        }
    }

    @Test
    void checkReceivedEquals() {
        var topic = "example";
        try (var consumer = connection.subscribe(topic)) {
            connection.send(topic, Event.ofValue("value1"), Event.ofValue("value2"));
            assertTrue(consumer.checkReceivedEqualsInTime(2, Duration.ofSeconds(1)));
        }
    }

    @Test
    void checkReceivedEqualsThrows() {
        var topic = "example";
        try (var consumer = connection.subscribe(topic)) {
            assertFalse(consumer.checkReceivedEqualsInTime(2, Duration.ofSeconds(1)));
        }
    }
}
