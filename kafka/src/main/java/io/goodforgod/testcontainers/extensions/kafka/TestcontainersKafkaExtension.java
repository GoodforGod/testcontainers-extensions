package io.goodforgod.testcontainers.extensions.kafka;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.*;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.DockerImageName;

@Internal
final class TestcontainersKafkaExtension extends
        AbstractTestcontainersExtension<KafkaConnection, GenericContainer, KafkaMetadata> {

    // https://docs.confluent.io/platform/7.0.0/release-notes/index.html#ak-raft-kraft
    private static final String MIN_KRAFT_TAG = "7.0.0";

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersKafkaExtension.class);

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerKafka.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ConnectionKafka.class;
    }

    @Override
    protected Class<KafkaConnection> getConnectionType() {
        return KafkaConnection.class;
    }

    @Override
    protected Class<GenericContainer> getContainerType() {
        return GenericContainer.class;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @Override
    protected GenericContainer createContainerDefault(KafkaMetadata metadata) {
        var image = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("confluentinc/cp-kafka"))
                .asCompatibleSubstituteFor(DockerImageName.parse("apache/kafka-native"));

        var container = (image.asCanonicalNameString().contains("confluentinc"))
                ? new org.testcontainers.containers.KafkaContainer(image).waitingFor(Wait.forListeningPort())
                : new KafkaContainer(image).waitingFor(Wait.forLogMessage(".*Startup complete.*", 1));
        final String alias = Optional.ofNullable(metadata.networkAlias()).orElseGet(() -> "kafka-" + System.currentTimeMillis());

        var loggerClass = (container instanceof org.testcontainers.containers.KafkaContainer okc)
                ? okc.getClass()
                : KafkaContainer.class;
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(loggerClass), true)
                .withMdc("image", image.asCanonicalNameString())
                .withMdc("alias", alias));
        container.withEnv("KAFKA_CONFLUENT_SUPPORT_METRICS_ENABLE", "false");
        container.withEnv("AUTO_CREATE_TOPICS", "true");
        container.withEnv("KAFKA_LOG4J_LOGGERS",
                "org.apache.zookeeper=ERROR,org.kafka.zookeeper=ERROR,kafka.zookeeper=ERROR,org.apache.kafka=ERROR,kafka=ERROR,kafka.network=ERROR,kafka.cluster=ERROR,kafka.controller=ERROR,kafka.coordinator=INFO,kafka.log=ERROR,kafka.server=ERROR,state.change.logger=ERROR");
        container.withStartupTimeout(Duration.ofMinutes(2));

        var actualVersion = new ComparableVersion(DockerImageName.parse(container.getDockerImageName()).getVersionPart());
        if (container instanceof org.testcontainers.containers.KafkaContainer oldKC) {
            container.withExposedPorts(KafkaConnectionImpl.KAFKA_PORT, org.testcontainers.containers.KafkaContainer.KAFKA_PORT);
            oldKC.withEnv("ZOOKEEPER_LOG4J_LOGGERS",
                    "org.apache.zookeeper=ERROR,org.kafka.zookeeper=ERROR,org.kafka.zookeeper.server=ERROR,kafka.zookeeper=ERROR,org.apache.kafka=ERROR");
            if (actualVersion.isLessThan(MIN_KRAFT_TAG)) {
                oldKC.withEmbeddedZookeeper();
            } else {
                oldKC.withKraft();
            }
        } else if (container instanceof ConfluentKafkaContainer ckc) {
            ckc.withEnv("ZOOKEEPER_LOG4J_LOGGERS",
                    "org.apache.zookeeper=ERROR,org.kafka.zookeeper=ERROR,org.kafka.zookeeper.server=ERROR,kafka.zookeeper=ERROR,org.apache.kafka=ERROR");
        } else {
            container.withExposedPorts(KafkaConnectionImpl.KAFKA_PORT, KafkaConnectionImpl.KAFKA_PORT);
        }

        container.setNetworkAliases(new ArrayList<>(List.of(alias)));
        if (metadata.networkShared()) {
            container.withNetwork(Network.SHARED);
        }

        return container;
    }

    @NotNull
    protected Optional<KafkaMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersKafka.class, context)
                .map(a -> new KafkaMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(),
                        Set.of(a.topics().value()), a.topics().reset()));
    }

    @Override
    protected void
            injectContextIntoField(ContainerContext<KafkaConnection> containerContext, Field field, Object testClassInstance) {
        try {
            final ConnectionKafka annotation = field.getAnnotation(ConnectionKafka.class);
            final KafkaConnectionImpl fieldKafkaConnection;
            if (annotation.properties().length == 0) {
                fieldKafkaConnection = (KafkaConnectionImpl) containerContext.connection();
            } else if (annotation.properties().length % 2 != 0) {
                throw new ExtensionConfigurationException(
                        "@ConnectionKafka#properties must have even number, properties expected as map of keys and values");
            } else {
                final Properties fieldProperties = new Properties();
                fieldProperties.putAll(containerContext.connection().params().properties());

                for (int i = 0; i < annotation.properties().length; i += 2) {
                    fieldProperties.put(annotation.properties()[i], annotation.properties()[i + 1]);
                }

                fieldKafkaConnection = (KafkaConnectionImpl) containerContext.connection().withProperties(fieldProperties);
                ((KafkaContext) containerContext).pool().add(fieldKafkaConnection);
            }

            field.setAccessible(true);
            field.set(testClassInstance, fieldKafkaConnection);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(String.format("Field '%s' annotated with @%s can't set kafka connection",
                    field.getName(), getConnectionAnnotation().getSimpleName()), e);
        }
    }

    @Override
    protected ContainerContext<KafkaConnection> createContainerContext(GenericContainer container) {
        return new KafkaContext(container);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        super.beforeAll(context);

        var metadata = getMetadata(context);
        if (!metadata.topics().isEmpty()) {
            ContainerContext<KafkaConnection> containerContext = getContainerContext(context);
            if (containerContext != null) {
                var connectionCurrent = containerContext.connection();
                var storage = getStorage(context);
                if (metadata.runMode() == ContainerMode.PER_RUN) {
                    connectionCurrent.createTopics(metadata.topics());
                    ((KafkaConnectionImpl) connectionCurrent).createTopicsIfNeeded(metadata.topics(),
                            metadata.reset() != Topics.Mode.NONE);
                    storage.put(Topics.class, metadata.reset());
                } else if (metadata.runMode() == ContainerMode.PER_CLASS) {
                    ((KafkaConnectionImpl) connectionCurrent).createTopicsIfNeeded(metadata.topics(), false);
                    storage.put(Topics.class, metadata.reset());
                }
            }
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var metadata = getMetadata(context);
        if (metadata.runMode() == ContainerMode.PER_METHOD && metadata.reset() == Topics.Mode.PER_CLASS) {
            throw new ExtensionConfigurationException(
                    String.format("@%s can't apply migration in Topics.Mode.PER_CLASS mode when ContainerMode.PER_METHOD is used",
                            getContainerAnnotation().getSimpleName()));
        }

        super.beforeEach(context);

        if (!metadata.topics().isEmpty()) {
            ContainerContext<KafkaConnection> containerContext = getContainerContext(context);
            if (containerContext != null) {
                var connectionCurrent = containerContext.connection();
                if (metadata.runMode() == ContainerMode.PER_METHOD) {
                    ((KafkaConnectionImpl) connectionCurrent).createTopicsIfNeeded(metadata.topics(), false);
                } else if (metadata.reset() == Topics.Mode.PER_METHOD) {
                    var storage = getStorage(context);
                    var createdTopicsFlag = storage.get(Topics.class, Topics.Mode.class);
                    if (createdTopicsFlag == null) {
                        ((KafkaConnectionImpl) connectionCurrent).createTopicsIfNeeded(metadata.topics(), true);
                    }
                }
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var metadata = getMetadata(context);
        var storage = getStorage(context);
        storage.remove(Topics.class);
        var containerContext = getContainerContext(context);
        if (metadata.runMode() != ContainerMode.PER_METHOD) {
            ((KafkaContext) containerContext).pool().clear();
        }

        super.afterEach(context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        super.afterAll(context);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context)
            throws ParameterResolutionException {
        final KafkaConnection connection = (KafkaConnection) super.resolveParameter(parameterContext, context);
        if (connection == null) {
            return null;
        }

        final ConnectionKafka annotation = parameterContext.getParameter().getAnnotation(ConnectionKafka.class);
        if (annotation.properties().length == 0) {
            return connection;
        }

        if (annotation.properties().length % 2 != 0) {
            throw new ExtensionConfigurationException(
                    "@ConnectionKafka#properties must have even number, properties expected as map of keys and values");
        }

        var properties = connection.params().properties();
        for (int i = 0; i < annotation.properties().length; i += 2) {
            properties.put(annotation.properties()[i], annotation.properties()[i + 1]);
        }

        var extensionContainer = getContainerContext(context);
        var paramConnection = connection.withProperties(properties);
        ((KafkaContext) extensionContainer).pool().add((KafkaConnectionImpl) paramConnection);
        return paramConnection;
    }
}
