package io.goodforgod.testcontainers.extensions.kafka;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.ExtensionContainer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.commons.util.ReflectionUtils;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

@Internal
final class TestcontainersKafkaExtension extends
        AbstractTestcontainersExtension<KafkaConnection, KafkaContainerExtra, KafkaMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersKafkaExtension.class);

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
    protected Class<KafkaContainerExtra> getContainerType() {
        return KafkaContainerExtra.class;
    }

    @Override
    protected KafkaContainerExtra getContainerDefault(KafkaMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("confluentinc/cp-kafka"));

        var container = new KafkaContainerExtra(dockerImage);

        container.setNetworkAliases(new ArrayList<>(List.of(metadata.networkAliasOrDefault())));
        if (metadata.networkShared()) {
            container.withNetwork(Network.SHARED);
        }

        return container;
    }

    @Override
    protected KafkaConnection getConnectionForContainer(KafkaMetadata metadata, KafkaContainerExtra container) {
        return container.connection();
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

        var metadata = getMetadata(context);
        var storage = getStorage(context);
        var extensionContainer = storage.get(metadata.runMode(), KafkaExtensionContainer.class);
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
                        fieldKafkaConnection = (KafkaConnectionImpl) kafkaConnection.withProperties(fieldProperties);
                        extensionContainer.pool().add(fieldKafkaConnection);
                    }

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
    protected ExtensionContainer<KafkaContainerExtra, KafkaConnection> getExtensionContainer(KafkaContainerExtra container,
                                                                                             KafkaConnection connection) {
        return new KafkaExtensionContainer(container, connection);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        super.beforeAll(context);

        var metadata = getMetadata(context);
        if (!metadata.topics().isEmpty()) {
            var connectionCurrent = getConnectionCurrent(context);
            var storage = getStorage(context);
            if (metadata.runMode() == ContainerMode.PER_RUN) {
                KafkaConnectionImpl.createTopicsIfNeeded(connectionCurrent, metadata.topics(),
                        metadata.reset() != Topics.Mode.NONE);
                storage.put(Topics.class, metadata.reset());
            } else if (metadata.runMode() == ContainerMode.PER_CLASS) {
                KafkaConnectionImpl.createTopicsIfNeeded(connectionCurrent, metadata.topics(), false);
                storage.put(Topics.class, metadata.reset());
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
            var connectionCurrent = getConnectionCurrent(context);
            if (metadata.runMode() == ContainerMode.PER_METHOD) {
                KafkaConnectionImpl.createTopicsIfNeeded(connectionCurrent, metadata.topics(), false);
            } else if (metadata.reset() == Topics.Mode.PER_METHOD) {
                var storage = getStorage(context);
                var createdTopicsFlag = storage.get(Topics.class, Topics.Mode.class);
                if (createdTopicsFlag == null) {
                    KafkaConnectionImpl.createTopicsIfNeeded(connectionCurrent, metadata.topics(), true);
                }
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var metadata = getMetadata(context);
        var storage = getStorage(context);
        storage.remove(Topics.class);
        var extensionContainer = storage.get(metadata.runMode(), KafkaExtensionContainer.class);
        if (metadata.runMode() != ContainerMode.PER_METHOD) {
            extensionContainer.pool().clear();
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

        final ContainerKafkaConnection annotation = parameterContext.getParameter().getAnnotation(ContainerKafkaConnection.class);
        if (annotation.properties().length == 0) {
            return connection;
        }

        var properties = connection.params().properties();
        for (ContainerKafkaConnection.Property property : annotation.properties()) {
            properties.put(property.name(), property.value());
        }

        var metadata = getMetadata(context);
        var storage = getStorage(context);
        var extensionContainer = storage.get(metadata.runMode(), KafkaExtensionContainer.class);
        var paramConnection = connection.withProperties(properties);
        extensionContainer.pool().add((KafkaConnectionImpl) paramConnection);
        return paramConnection;
    }
}
