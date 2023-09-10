package io.goodforgod.testcontainers.extensions.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class KafkaContainerExtra extends KafkaContainer {

    private static final String EXTERNAL_TEST_KAFKA_BOOTSTRAP = "EXTERNAL_TEST_KAFKA_BOOTSTRAP_SERVERS";
    private static final String EXTERNAL_TEST_KAFKA_PREFIX = "EXTERNAL_TEST_KAFKA_";

    private volatile KafkaConnectionImpl connection;

    public KafkaContainerExtra(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public KafkaContainerExtra(DockerImageName dockerImageName) {
        super(dockerImageName);

        final String alias = "kafka-" + System.currentTimeMillis();

        this.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(KafkaContainerExtra.class))
                .withMdc("image", dockerImageName.asCanonicalNameString())
                .withMdc("alias", alias));
        this.withEnv("KAFKA_CONFLUENT_SUPPORT_METRICS_ENABLE", "false");
        this.withEnv("AUTO_CREATE_TOPICS", "true");
        this.withEnv("KAFKA_LOG4J_LOGGERS",
                "org.apache.zookeeper=ERROR,org.kafka.zookeeper=ERROR,kafka.zookeeper=ERROR,org.apache.kafka=ERROR,kafka=ERROR,kafka.network=ERROR,kafka.cluster=ERROR,kafka.controller=ERROR,kafka.coordinator=INFO,kafka.log=ERROR,kafka.server=ERROR,state.change.logger=ERROR");
        this.withEnv("ZOOKEEPER_LOG4J_LOGGERS",
                "org.apache.zookeeper=ERROR,org.kafka.zookeeper=ERROR,org.kafka.zookeeper.server=ERROR,kafka.zookeeper=ERROR,org.apache.kafka=ERROR");
        this.withEmbeddedZookeeper();
        this.withExposedPorts(9092, KafkaContainer.KAFKA_PORT);
        this.waitingFor(Wait.forListeningPort());
        this.withStartupTimeout(Duration.ofMinutes(5));

        this.setNetworkAliases(new ArrayList<>(List.of(alias)));
    }

    @NotNull
    public KafkaConnection connection() {
        if (connection == null) {
            final Optional<KafkaConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !isRunning()) {
                throw new IllegalStateException("KafkaConnection can't be create for container that is not running");
            }

            final KafkaConnection jdbcConnection = connectionExternal.orElseGet(() -> {
                final String alias = getNetworkAliases().get(getNetworkAliases().size() - 1);

                final Properties properties = new Properties();
                properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());

                final Properties networkProperties = new Properties();
                networkProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, String.format("%s:%s", alias, "9092"));

                return new KafkaConnectionImpl(properties, networkProperties);
            });

            this.connection = (KafkaConnectionImpl) jdbcConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<KafkaConnection> connectionExternal = getConnectionExternal();
        if (connectionExternal.isEmpty()) {
            super.start();
        }
    }

    @Override
    public void stop() {
        connection.close();
        connection = null;
        super.stop();
    }

    @NotNull
    private static Optional<KafkaConnection> getConnectionExternal() {
        var bootstrap = System.getenv(EXTERNAL_TEST_KAFKA_BOOTSTRAP);
        if (bootstrap != null) {
            final Properties properties = new Properties();
            System.getenv().forEach((k, v) -> {
                if (k.startsWith(EXTERNAL_TEST_KAFKA_PREFIX)) {
                    var name = k.replace(EXTERNAL_TEST_KAFKA_PREFIX, "").replace("_", ".").toLowerCase();
                    properties.put(name, v);
                }
            });

            return Optional.of(new KafkaConnectionImpl(properties, null));
        } else {
            return Optional.empty();
        }
    }
}
