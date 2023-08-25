package io.goodforgod.testcontainers.extensions.kafka;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.commons.util.ReflectionUtils;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@Internal
final class TestcontainersKafkaExtension extends
        AbstractTestcontainersExtension<KafkaConnection, KafkaContainer, KafkaMetadata> {

    private static final String EXTERNAL_TEST_KAFKA_BOOTSTRAP = "EXTERNAL_TEST_KAFKA_BOOTSTRAP_SERVERS";
    private static final String EXTERNAL_TEST_KAFKA_PREFIX = "EXTERNAL_TEST_KAFKA_";

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersKafkaExtension.class);

    private static final class KafkaConnectionPool {

        private final List<KafkaConnectionImpl> connections = new ArrayList<>();

        private void add(KafkaConnectionImpl connection) {
            connections.add(connection);
        }

        private void clear() {
            for (KafkaConnectionImpl connection : connections) {
                try {
                    connection.clear();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }

        private void close() {
            for (KafkaConnectionImpl connection : connections) {
                try {
                    connection.close();
                } catch (Exception e) {
                    // do nothing
                }
            }

            connections.clear();
        }
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerKafka.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ContainerKafkaConnection.class;
    }

    @Override
    protected Class<KafkaConnection> getConnectionType() {
        return KafkaConnection.class;
    }

    @Override
    protected Class<KafkaContainer> getContainerType() {
        return KafkaContainer.class;
    }

    @Override
    protected KafkaContainer getContainerDefault(KafkaMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("confluentinc/cp-kafka"));

        var container = new KafkaContainer(dockerImage)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(KafkaContainer.class))
                        .withMdc("image", metadata.image())
                        .withMdc("alias", metadata.networkAliasOrDefault()))
                .withEnv("KAFKA_CONFLUENT_SUPPORT_METRICS_ENABLE", "false")
                .withEnv("AUTO_CREATE_TOPICS", "true")
                .withEmbeddedZookeeper()
                .withNetworkAliases(metadata.networkAliasOrDefault())
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(5));

        if (metadata.networkShared()) {
            container.withNetwork(Network.SHARED);
        }

        return container;
    }

    @Override
    protected KafkaConnection getConnectionForContainer(KafkaMetadata metadata, KafkaContainer container) {
        final Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers());

        final Properties networkProperties = container.getNetworkAliases().stream()
                .filter(a -> a.equals(metadata.networkAliasOrDefault()))
                .findFirst()
                .or(() -> container.getNetworkAliases().stream().findFirst())
                .map(alias -> {
                    final Properties props = new Properties();
                    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                            String.format("%s:%s", alias, "9092"));
                    return props;
                })
                .orElse(null);

        return new KafkaConnectionImpl(properties, networkProperties);
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @NotNull
    protected Optional<KafkaMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersKafka.class, context)
                .map(a -> new KafkaMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(),
                        Set.of(a.topics().value()), a.topics().reset()));
    }

    @NotNull
    protected Optional<KafkaConnection> getConnectionExternal() {
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

    @Override
    protected void injectConnection(KafkaConnection kafkaConnection, ExtensionContext context) {
        var annotationProducer = getConnectionAnnotation();
        var connectionFields = ReflectionUtils.findFields(context.getRequiredTestClass(),
                f -> !f.isSynthetic()
                        && !Modifier.isFinal(f.getModifiers())
                        && !Modifier.isStatic(f.getModifiers())
                        && f.getAnnotation(annotationProducer) != null,
                ReflectionUtils.HierarchyTraversalMode.TOP_DOWN);

        logger.debug("Starting @ContainerKafkaConnection field injection for container properties: {}", kafkaConnection);

        var storage = context.getStore(NAMESPACE);
        var pool = storage.get(KafkaConnectionPool.class, KafkaConnectionPool.class);
        context.getTestInstance().ifPresent(instance -> {
            for (Field field : connectionFields) {
                try {
                    final ContainerKafkaConnection annotation = field.getAnnotation(ContainerKafkaConnection.class);
                    final KafkaConnectionImpl fieldKafkaConnection;
                    if (annotation.properties().length == 0) {
                        fieldKafkaConnection = (KafkaConnectionImpl) kafkaConnection;
                    } else {
                        final Properties fieldProperties = new Properties();
                        fieldProperties.putAll(kafkaConnection.params().properties());
                        Arrays.stream(annotation.properties())
                                .forEach(property -> fieldProperties.put(property.name(), property.value()));

                        final Properties fieldNetworkProperties;
                        if (kafkaConnection.paramsInNetwork().isEmpty()) {
                            fieldNetworkProperties = null;
                        } else {
                            fieldNetworkProperties = new Properties();
                            fieldNetworkProperties.putAll(kafkaConnection.paramsInNetwork().get().properties());
                            Arrays.stream(annotation.properties())
                                    .forEach(property -> fieldNetworkProperties.put(property.name(), property.value()));
                        }

                        fieldKafkaConnection = new KafkaConnectionImpl(fieldProperties, fieldNetworkProperties);
                    }

                    pool.add(fieldKafkaConnection);

                    field.setAccessible(true);
                    field.set(instance, fieldKafkaConnection);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(String.format("Field '%s' annotated with @%s can't set kafka connection",
                            field.getName(), annotationProducer.getSimpleName()), e);
                }
            }
        });
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        var storage = getStorage(context);
        storage.put(KafkaConnectionPool.class, new KafkaConnectionPool());

        super.beforeAll(context);

        var metadata = getMetadata(context);
        if (!metadata.topics().isEmpty()) {
            var connectionCurrent = getConnectionCurrent(context);
            if (metadata.runMode() == ContainerMode.PER_RUN) {
                KafkaConnectionImpl.createTopicsIfNeeded(connectionCurrent, metadata.topics(),
                        metadata.reset() == Topics.Mode.PER_CLASS);
            } else if (metadata.runMode() == ContainerMode.PER_CLASS) {
                KafkaConnectionImpl.createTopicsIfNeeded(connectionCurrent, metadata.topics(), false);
            }
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        super.beforeEach(context);

        var metadata = getMetadata(context);
        if (!metadata.topics().isEmpty()) {
            if (metadata.runMode() == ContainerMode.PER_METHOD) {
                var connectionCurrent = getConnectionCurrent(context);
                KafkaConnectionImpl.createTopicsIfNeeded(connectionCurrent, metadata.topics(), false);
            } else if (metadata.reset() == Topics.Mode.PER_METHOD) {
                var connectionCurrent = getConnectionCurrent(context);
                KafkaConnectionImpl.createTopicsIfNeeded(connectionCurrent, metadata.topics(), true);
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var storage = getStorage(context);
        var pool = storage.get(KafkaConnectionPool.class, KafkaConnectionPool.class);
        pool.clear();

        super.afterEach(context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        var storage = getStorage(context);
        var pool = storage.get(KafkaConnectionPool.class, KafkaConnectionPool.class);
        pool.close();

        super.afterAll(context);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context)
            throws ParameterResolutionException {
        final KafkaConnection connection = (KafkaConnection) super.resolveParameter(parameterContext, context);
        if (connection == null) {
            return null;
        }

        var properties = connection.params().properties();
        var networkProperties = connection.paramsInNetwork().isEmpty()
                ? null
                : connection.paramsInNetwork().get().properties();

        final ContainerKafkaConnection annotation = parameterContext.getParameter().getAnnotation(ContainerKafkaConnection.class);
        for (ContainerKafkaConnection.Property property : annotation.properties()) {
            properties.put(property.name(), property.value());
            if (networkProperties != null) {
                networkProperties.put(property.name(), property.value());
            }
        }

        return new KafkaConnectionImpl(properties, networkProperties);
    }
}
