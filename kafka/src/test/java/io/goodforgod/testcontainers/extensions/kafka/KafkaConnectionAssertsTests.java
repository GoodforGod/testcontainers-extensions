package io.goodforgod.testcontainers.extensions.kafka;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.time.Duration;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.opentest4j.AssertionFailedError;

@TestcontainersKafka(mode = ContainerMode.PER_CLASS, image = "confluentinc/cp-kafka:7.4.1")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaConnectionAssertsTests {

    @ContainerKafkaConnection
    private KafkaConnection connection;

    @Test
    void getReceived() {
        // given
        var topic = "example";
        var consumer = connection.subscribe(topic);

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
        assertNotEquals(topic, received.get().topic());
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
    }

    @Test
    void getReceivedAtLeast() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        connection.send(topic, Event.ofValue("value1"), Event.ofValue("value2"));
        var received = consumer.getReceived(2, Duration.ofSeconds(1));
        assertEquals(2, received.size());
        assertNotEquals(received.get(0), received.get(1));
        assertNotEquals(received.get(0).toString(), received.get(1).toString());
    }

    @Test
    void assertReceivedNone() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        consumer.assertReceivedNone(Duration.ofSeconds(1));
    }

    @Test
    void assertReceivedNoneThrows() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        connection.send(topic, Event.ofValue("value"));
        assertThrows(AssertionFailedError.class, () -> consumer.assertReceivedNone(Duration.ofSeconds(1)));
    }

    @Test
    void assertReceived() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        connection.send(topic, Event.ofValue("value"));
        consumer.assertReceivedAtLeast(Duration.ofSeconds(1));
    }

    @Test
    void assertReceivedThrows() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        assertThrows(AssertionFailedError.class, () -> consumer.assertReceivedAtLeast(Duration.ofSeconds(1)));
    }

    @Test
    void assertReceivedAtLeast() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        connection.send(topic, Event.ofValue("value1"), Event.ofValue("value2"));
        consumer.assertReceivedAtLeast(2, Duration.ofSeconds(1));
    }

    @Test
    void assertReceivedAtLeastThrows() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        assertThrows(AssertionFailedError.class, () -> consumer.assertReceivedAtLeast(2, Duration.ofSeconds(1)));
    }

    @Test
    void assertReceivedEquals() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        connection.send(topic, Event.ofValue("value1"), Event.ofValue("value2"));
        consumer.assertReceivedEqualsInTime(2, Duration.ofSeconds(1));
    }

    @Test
    void assertReceivedEqualsThrows() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        assertThrows(AssertionFailedError.class, () -> consumer.assertReceivedEqualsInTime(2, Duration.ofSeconds(1)));
    }

    @Test
    void checkReceivedNone() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        assertTrue(consumer.checkReceivedNone(Duration.ofSeconds(1)));
    }

    @Test
    void checkReceivedNoneThrows() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        connection.send(topic, Event.ofValue("value"));
        assertFalse(consumer.checkReceivedNone(Duration.ofSeconds(1)));
    }

    @Test
    void checkReceivedAtLeast() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        connection.send(topic, Event.ofValue("value1"), Event.ofValue("value2"));
        assertTrue(consumer.checkReceivedAtLeast(2, Duration.ofSeconds(1)));
    }

    @Test
    void checkReceivedAtLeastThrows() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        assertFalse(consumer.checkReceivedAtLeast(2, Duration.ofSeconds(1)));
    }

    @Test
    void checkReceivedEquals() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        connection.send(topic, Event.ofValue("value1"), Event.ofValue("value2"));
        assertTrue(consumer.checkReceivedEqualsInTime(2, Duration.ofSeconds(1)));
    }

    @Test
    void checkReceivedEqualsThrows() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        assertFalse(consumer.checkReceivedEqualsInTime(2, Duration.ofSeconds(1)));
    }
}
