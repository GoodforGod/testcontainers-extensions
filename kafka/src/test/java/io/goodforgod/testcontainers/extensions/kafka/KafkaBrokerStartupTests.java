package io.goodforgod.testcontainers.extensions.kafka;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.util.Set;
import org.junit.jupiter.api.Test;

class KafkaBrokerStartupTests {

    @Test
    void startsWithoutStartupLogMessage() {
        var extension = new TestcontainersKafkaExtension();
        var metadata = new KafkaMetadata(false, "kafka-startup", "apache/kafka-native:4.3.1",
                ContainerMode.PER_METHOD, Set.of(), Topics.Mode.NONE);
        try (var container = extension.createContainerDefault(metadata)) {
            // Suppress the TransactionCoordinator message used by the previous wait strategy.
            container.withEnv("KAFKA_LOG4J_LOGGERS", "kafka=OFF,org.apache.kafka=OFF");
            container.start();
            var context = extension.createContainerContext(container);
            try {
                context.connection().createTopics(Set.of("startup-check"));
                assertTrue(container.isRunning());
                assertFalse(container.getLogs().contains("Startup complete"));
            } finally {
                context.stop();
            }
        }
    }
}
