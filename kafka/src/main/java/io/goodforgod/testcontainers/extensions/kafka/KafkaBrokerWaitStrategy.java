package io.goodforgod.testcontainers.extensions.kafka;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.shaded.org.awaitility.Awaitility;

/**
 * Waits for broker metadata through the published client listener, independently of broker logging.
 */
final class KafkaBrokerWaitStrategy extends AbstractWaitStrategy {

    @Override
    protected void waitUntilReady() {
        var bootstrap = waitStrategyTarget.getHost() + ":"
                + waitStrategyTarget.getMappedPort(KafkaConnectionImpl.KAFKA_PORT);
        var admin = Admin.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "1000",
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "1000"));
        try {
            Awaitility.await()
                    .pollInterval(Duration.ofMillis(100))
                    .atMost(startupTimeout)
                    .ignoreExceptions()
                    .until(() -> !admin.describeCluster().nodes().get(1, TimeUnit.SECONDS).isEmpty());
        } finally {
            admin.close(Duration.ZERO);
        }
    }
}
