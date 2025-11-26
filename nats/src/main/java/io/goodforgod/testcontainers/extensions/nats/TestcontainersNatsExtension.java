package io.goodforgod.testcontainers.extensions.nats;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.testcontainers.nats.NatsContainer;
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
import org.testcontainers.utility.DockerImageName;

@Internal
final class TestcontainersNatsExtension extends
        AbstractTestcontainersExtension<NatsConnection, GenericContainer, NatsMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersNatsExtension.class);

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerNats.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ConnectionNats.class;
    }

    @Override
    protected Class<NatsConnection> getConnectionType() {
        return NatsConnection.class;
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
    protected GenericContainer createContainerDefault(NatsMetadata metadata) {
        var image = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("nats"));

        var container = new NatsContainer(image).waitingFor(Wait.forListeningPort());
        final String alias = Optional.ofNullable(metadata.networkAlias()).orElseGet(() -> "nats-" + System.currentTimeMillis());

        var loggerClass = NatsContainer.class;
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(loggerClass), true)
                .withMdc("image", image.asCanonicalNameString())
                .withMdc("alias", alias));
        container.withStartupTimeout(Duration.ofMinutes(2));

        container.setNetworkAliases(new ArrayList<>(List.of(alias)));
        if (metadata.networkShared()) {
            container.withNetwork(Network.SHARED);
        }

        return container;
    }

    @NotNull
    protected Optional<NatsMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersNats.class, context)
                .map(a -> new NatsMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode()));
    }

    @Override
    protected void
            injectContextIntoField(ContainerContext<NatsConnection> containerContext, Field field, Object testClassInstance) {
        try {
            final ConnectionNats annotation = field.getAnnotation(ConnectionNats.class);
            final NatsConnectionImpl fieldNatsConnection;
            if (annotation.properties().length == 0) {
                fieldNatsConnection = (NatsConnectionImpl) containerContext.connection();
            } else if (annotation.properties().length % 2 != 0) {
                throw new ExtensionConfigurationException(
                        "@ConnectionNats#properties must have even number, properties expected as map of keys and values");
            } else {
                final Properties fieldProperties = new Properties();
                fieldProperties.putAll(containerContext.connection().params().properties());

                for (int i = 0; i < annotation.properties().length; i += 2) {
                    fieldProperties.put(annotation.properties()[i], annotation.properties()[i + 1]);
                }

                fieldNatsConnection = (NatsConnectionImpl) containerContext.connection().withProperties(fieldProperties);
                ((NatsContext) containerContext).pool().add(fieldNatsConnection);
            }

            field.setAccessible(true);
            field.set(testClassInstance, fieldNatsConnection);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(String.format("Field '%s' annotated with @%s can't set nats connection",
                    field.getName(), getConnectionAnnotation().getSimpleName()), e);
        }
    }

    @Override
    protected ContainerContext<NatsConnection> createContainerContext(GenericContainer container) {
        return new NatsContext(container);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var metadata = getMetadata(context);
        var containerContext = getContainerContext(context);
        if (metadata.runMode() != ContainerMode.PER_METHOD) {
            ((NatsContext) containerContext).pool().clear();
        }

        super.afterEach(context);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context)
            throws ParameterResolutionException {
        final NatsConnection connection = (NatsConnection) super.resolveParameter(parameterContext, context);
        if (connection == null) {
            return null;
        }

        final ConnectionNats annotation = parameterContext.getParameter().getAnnotation(ConnectionNats.class);
        if (annotation.properties().length == 0) {
            return connection;
        }

        if (annotation.properties().length % 2 != 0) {
            throw new ExtensionConfigurationException(
                    "@ConnectionNats#properties must have even number, properties expected as map of keys and values");
        }

        var properties = connection.params().properties();
        for (int i = 0; i < annotation.properties().length; i += 2) {
            properties.put(annotation.properties()[i], annotation.properties()[i + 1]);
        }

        var extensionContainer = getContainerContext(context);
        var paramConnection = connection.withProperties(properties);
        ((NatsContext) extensionContainer).pool().add((NatsConnectionImpl) paramConnection);
        return paramConnection;
    }
}
