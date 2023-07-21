package io.goodforgod.testcontainers.extensions.kafka;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;

@TestcontainersKafka(mode = ContainerMode.PER_METHOD, image = "confluentinc/cp-kafka:7.4.1")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContainerFromAnnotationTests {

    @ContainerKafka
    private static final KafkaContainer container = new KafkaContainer()
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(KafkaContainer.class)))
            .withNetwork(Network.SHARED);

    @Test
    void checkParams(@ContainerKafkaConnection KafkaConnection connection) {
        assertNotNull(connection.properties());
    }
}
