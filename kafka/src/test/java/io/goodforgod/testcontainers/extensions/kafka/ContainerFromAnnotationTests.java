package io.goodforgod.testcontainers.extensions.kafka;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@TestcontainersKafka(mode = ContainerMode.PER_METHOD, image = "confluentinc/cp-kafka:7.4.1")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerFromAnnotationTests {

    @ContainerKafka
    private static final KafkaContainer container = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.1"))
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(KafkaContainer.class)))
            .withNetwork(Network.SHARED);

    @Test
    void checkParams(@ContainerKafkaConnection KafkaConnection connection) {
        assertNotNull(connection.params().boostrapServers());
        assertNotNull(connection.params().properties());
        assertTrue(connection.paramsInNetwork().isPresent());
        assertNotNull(connection.paramsInNetwork().get().boostrapServers());
        assertNotEquals(connection.params().boostrapServers(), connection.paramsInNetwork().get().boostrapServers());
        assertNotNull(connection.paramsInNetwork().get().properties());
    }
}
