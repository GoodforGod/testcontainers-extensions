package io.goodforgod.testcontainers.extensions.redpanda;

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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;

@Internal
final class TestcontainersRedpandaExtension extends
        AbstractTestcontainersExtension<RedpandaConnection, RedpandaContainer, RedpandaMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersRedpandaExtension.class);

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerRedpanda.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ConnectionRedpanda.class;
    }

    @Override
    protected Class<RedpandaConnection> getConnectionType() {
        return RedpandaConnection.class;
    }

    @Override
    protected Class<RedpandaContainer> getContainerType() {
        return RedpandaContainer.class;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @Override
    protected RedpandaContainer createContainerDefault(RedpandaMetadata metadata) {
        var image = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("redpandadata/redpanda"));

        var container = new RedpandaExtraContainer(image);
        final String alias = Optional.ofNullable(metadata.networkAlias())
                .orElseGet(() -> "redpanda-" + System.currentTimeMillis());

        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(RedpandaContainer.class))
                .withMdc("image", image.asCanonicalNameString())
                .withMdc("alias", alias));
        container.withExposedPorts(RedpandaConnectionImpl.REDPANDA_PORT, RedpandaConnectionImpl.REDPANDA_PORT);
        container.withStartupTimeout(Duration.ofMinutes(2));

        container.setNetworkAliases(new ArrayList<>(List.of(alias)));
        if (metadata.networkShared()) {
            container.withNetwork(Network.SHARED);
        }

        return container;
    }

    @NotNull
    protected Optional<RedpandaMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersRedpanda.class, context)
                .map(a -> new RedpandaMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(),
                        Set.of(a.topics().value()), a.topics().reset()));
    }

    @Override
    protected void
            injectContextIntoField(ContainerContext<RedpandaConnection> containerContext, Field field, Object testClassInstance) {
        try {
            final ConnectionRedpanda annotation = field.getAnnotation(ConnectionRedpanda.class);
            final RedpandaConnectionImpl fieldConnection;
            if (annotation.properties().length == 0) {
                fieldConnection = (RedpandaConnectionImpl) containerContext.connection();
            } else if (annotation.properties().length % 2 != 0) {
                throw new ExtensionConfigurationException(
                        "@ConnectionRedpanda#properties must have even number, properties expected as map of keys and values");
            } else {
                final Properties fieldProperties = new Properties();
                fieldProperties.putAll(containerContext.connection().params().properties());

                for (int i = 0; i < annotation.properties().length; i += 2) {
                    fieldProperties.put(annotation.properties()[i], annotation.properties()[i + 1]);
                }

                fieldConnection = (RedpandaConnectionImpl) containerContext.connection().withProperties(fieldProperties);
                ((RedpandaContext) containerContext).pool().add(fieldConnection);
            }

            field.setAccessible(true);
            field.set(testClassInstance, fieldConnection);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(String.format("Field '%s' annotated with @%s can't set RedpandaConnection",
                    field.getName(), getConnectionAnnotation().getSimpleName()), e);
        }
    }

    @Override
    protected ContainerContext<RedpandaConnection> createContainerContext(RedpandaContainer container) {
        return new RedpandaContext(container);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        super.beforeAll(context);

        var metadata = getMetadata(context);
        if (!metadata.topics().isEmpty()) {
            ContainerContext<RedpandaConnection> containerContext = getContainerContext(context);
            if (containerContext != null) {
                var connectionCurrent = containerContext.connection();
                var storage = getStorage(context);
                if (metadata.runMode() == ContainerMode.PER_RUN) {
                    connectionCurrent.createTopics(metadata.topics());
                    ((RedpandaConnectionImpl) connectionCurrent).createTopicsIfNeeded(metadata.topics(),
                            metadata.reset() != Topics.Mode.NONE);
                    storage.put(Topics.class, metadata.reset());
                } else if (metadata.runMode() == ContainerMode.PER_CLASS) {
                    ((RedpandaConnectionImpl) connectionCurrent).createTopicsIfNeeded(metadata.topics(), false);
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
            ContainerContext<RedpandaConnection> containerContext = getContainerContext(context);
            if (containerContext != null) {
                var connectionCurrent = containerContext.connection();
                if (metadata.runMode() == ContainerMode.PER_METHOD) {
                    ((RedpandaConnectionImpl) connectionCurrent).createTopicsIfNeeded(metadata.topics(), false);
                } else if (metadata.reset() == Topics.Mode.PER_METHOD) {
                    var storage = getStorage(context);
                    var createdTopicsFlag = storage.get(Topics.class, Topics.Mode.class);
                    if (createdTopicsFlag == null) {
                        ((RedpandaConnectionImpl) connectionCurrent).createTopicsIfNeeded(metadata.topics(), true);
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
            ((RedpandaContext) containerContext).pool().clear();
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
        final RedpandaConnection connection = (RedpandaConnection) super.resolveParameter(parameterContext, context);
        if (connection == null) {
            return null;
        }

        final ConnectionRedpanda annotation = parameterContext.getParameter().getAnnotation(ConnectionRedpanda.class);
        if (annotation.properties().length == 0) {
            return connection;
        }

        if (annotation.properties().length % 2 != 0) {
            throw new ExtensionConfigurationException(
                    "@ConnectionRedpanda#properties must have even number, properties expected as map of keys and values");
        }

        var properties = connection.params().properties();
        for (int i = 0; i < annotation.properties().length; i += 2) {
            properties.put(annotation.properties()[i], annotation.properties()[i + 1]);
        }

        var extensionContainer = getContainerContext(context);
        var paramConnection = connection.withProperties(properties);
        ((RedpandaContext) extensionContainer).pool().add((RedpandaConnectionImpl) paramConnection);
        return paramConnection;
    }
}
