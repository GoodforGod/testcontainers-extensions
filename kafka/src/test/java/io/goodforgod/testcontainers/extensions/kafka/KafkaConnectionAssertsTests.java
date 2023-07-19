package io.goodforgod.testcontainers.extensions.kafka;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.time.Duration;
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
        var topic = "example";
        var consumer = connection.subscribe(topic);
        connection.send(topic, Event.ofValue("value"));
        var received = consumer.getReceived(Duration.ofSeconds(1));
        assertTrue(received.isPresent());
    }

    @Test
    void getReceivedAtLeast() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        connection.send(topic, Event.ofValue("value1"), Event.ofValue("value2"));
        var received = consumer.getReceivedAtLeast(2, Duration.ofSeconds(1));
        assertEquals(2, received.size());
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
        consumer.assertReceived(Duration.ofSeconds(1));
    }

    @Test
    void assertReceivedThrows() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        assertThrows(AssertionFailedError.class, () -> consumer.assertReceived(Duration.ofSeconds(1)));
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
    void checkReceived() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        connection.send(topic, Event.ofValue("value"));
        assertTrue(consumer.checkReceived(Duration.ofSeconds(1)));
    }

    @Test
    void checkReceivedThrows() {
        var topic = "example";
        var consumer = connection.subscribe(topic);
        assertFalse(consumer.checkReceived(Duration.ofSeconds(1)));
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
