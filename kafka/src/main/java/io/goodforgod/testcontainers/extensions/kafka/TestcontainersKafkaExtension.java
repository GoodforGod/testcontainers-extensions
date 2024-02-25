package io.goodforgod.testcontainers.extensions.kafka;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.DockerImageName;

@Internal
final class TestcontainersKafkaExtension extends
        AbstractTestcontainersExtension<KafkaConnection, KafkaContainer, KafkaMetadata> {

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
    protected Class<KafkaContainer> getContainerType() {
        return KafkaContainer.class;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @Override
    protected KafkaContainer createContainerDefault(KafkaMetadata metadata) {
        var image = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("confluentinc/cp-kafka"));

        var container = new KafkaContainer(image);
        final String alias = Optional.ofNullable(metadata.networkAlias()).orElseGet(() -> "kafka-" + System.currentTimeMillis());

        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(KafkaContainer.class))
                .withMdc("image", image.asCanonicalNameString())
                .withMdc("alias", alias));
        container.withEnv("KAFKA_CONFLUENT_SUPPORT_METRICS_ENABLE", "false");
        container.withEnv("AUTO_CREATE_TOPICS", "true");
        container.withEnv("KAFKA_LOG4J_LOGGERS",
                "org.apache.zookeeper=ERROR,org.kafka.zookeeper=ERROR,kafka.zookeeper=ERROR,org.apache.kafka=ERROR,kafka=ERROR,kafka.network=ERROR,kafka.cluster=ERROR,kafka.controller=ERROR,kafka.coordinator=INFO,kafka.log=ERROR,kafka.server=ERROR,state.change.logger=ERROR");
        container.withEnv("ZOOKEEPER_LOG4J_LOGGERS",
                "org.apache.zookeeper=ERROR,org.kafka.zookeeper=ERROR,org.kafka.zookeeper.server=ERROR,kafka.zookeeper=ERROR,org.apache.kafka=ERROR");
        container.withExposedPorts(9092, KafkaContainer.KAFKA_PORT);
        container.waitingFor(Wait.forListeningPort());
        container.withStartupTimeout(Duration.ofMinutes(5));

        var actualVersion = new ComparableVersion(DockerImageName.parse(container.getDockerImageName()).getVersionPart());
        if (!actualVersion.isLessThan(MIN_KRAFT_TAG)) {
            final Optional<Method> withKraft = Arrays.stream(KafkaContainer.class.getDeclaredMethods())
                    .filter(m -> m.getName().equals("withKraft"))
                    .findFirst();

            if (withKraft.isPresent()) {
                withKraft.get().setAccessible(true);
                try {
                    withKraft.get().invoke(this);
                    LoggerFactory.getLogger(KafkaContainer.class).info("Kraft is enabled");
                } catch (IllegalAccessException | InvocationTargetException e) {
                    container.withEmbeddedZookeeper();
                }
            } else {
                container.withEmbeddedZookeeper();
            }
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
            } else {
                final Properties fieldProperties = new Properties();
                fieldProperties.putAll(containerContext.connection().params().properties());
                Arrays.stream(annotation.properties())
                        .forEach(property -> fieldProperties.put(property.name(), property.value()));
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
    protected ContainerContext<KafkaConnection> createContainerContext(KafkaContainer container) {
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

        var properties = connection.params().properties();
        for (ConnectionKafka.Property property : annotation.properties()) {
            properties.put(property.name(), property.value());
        }

        var extensionContainer = getContainerContext(context);
        var paramConnection = connection.withProperties(properties);
        ((KafkaContext) extensionContainer).pool().add((KafkaConnectionImpl) paramConnection);
        return paramConnection;
    }
}
