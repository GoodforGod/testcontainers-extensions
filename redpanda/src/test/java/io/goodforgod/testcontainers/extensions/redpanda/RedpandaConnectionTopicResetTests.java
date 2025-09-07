package io.goodforgod.testcontainers.extensions.redpanda;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.time.Duration;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestcontainersRedpanda(mode = ContainerMode.PER_CLASS,
        image = "redpandadata/redpanda:v25.1.11",
        topics = @Topics(value = "my-topic", reset = Topics.Mode.PER_METHOD))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RedpandaConnectionTopicResetTests {

    @ConnectionRedpanda(properties = { ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest" })
    private RedpandaConnection connection;

    @Order(1)
    @Test
    void firstConnection() {
        // given
        assertTrue(connection.params().properties().containsKey(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
        try (var consumer = connection.subscribe("my-topic")) {
            // when
            connection.send("my-topic", Event.ofValue("1"));

            // then
            consumer.assertReceivedEqualsInTime(1, Duration.ofMillis(100));
        }
    }

    @Order(2)
    @Test
    void secondConnection() {
        // given
        assertTrue(connection.params().properties().containsKey(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
        try (var consumer = connection.subscribe("my-topic")) {
            // when
            connection.send("my-topic", Event.ofValue("1"));

            // then
            consumer.assertReceivedEqualsInTime(1, Duration.ofMillis(100));
        }
    }
}
