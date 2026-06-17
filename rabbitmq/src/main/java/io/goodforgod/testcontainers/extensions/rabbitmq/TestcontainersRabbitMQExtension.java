package io.goodforgod.testcontainers.extensions.rabbitmq;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@Internal
final class TestcontainersRabbitMQExtension extends
        AbstractTestcontainersExtension<RabbitMQConnection, GenericContainer, RabbitMQMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersRabbitMQExtension.class);

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerRabbitMQ.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ConnectionRabbitMQ.class;
    }

    @Override
    protected Class<RabbitMQConnection> getConnectionType() {
        return RabbitMQConnection.class;
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
    protected GenericContainer createContainerDefault(RabbitMQMetadata metadata) {
        var image = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("rabbitmq"));

        var container = new RabbitMQContainer(image);
        final String alias = Optional.ofNullable(metadata.networkAlias()).orElseGet(() -> "rabbitmq-" + System.currentTimeMillis());

        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(RabbitMQContainer.class), true)
                .withMdc("image", image.asCanonicalNameString())
                .withMdc("alias", alias));
        container.withStartupTimeout(Duration.ofMinutes(2));

        container.setNetworkAliases(new ArrayList<>(List.of(alias)));
        if (metadata.networkShared()) {
            container.withNetwork(Network.SHARED);
        }

        return container;
    }

    @Override
    protected @NotNull Optional<RabbitMQMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersRabbitMQ.class, context)
                .map(a -> new RabbitMQMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(),
                        toTopology(a.topology()), a.topology().reset()));
    }

    @Override
    protected void injectContextIntoField(ContainerContext<RabbitMQConnection> containerContext, Field field,
                                          Object testClassInstance) {
        try {
            final ConnectionRabbitMQ annotation = field.getAnnotation(ConnectionRabbitMQ.class);
            final RabbitMQConnectionImpl fieldConnection;
            if (annotation.properties().length == 0) {
                fieldConnection = (RabbitMQConnectionImpl) containerContext.connection();
            } else if (annotation.properties().length % 2 != 0) {
                throw new ExtensionConfigurationException(
                        "@ConnectionRabbitMQ#properties must have even number, properties expected as map of keys and values");
            } else {
                final Properties fieldProperties = new Properties();
                fieldProperties.putAll(containerContext.connection().params().properties());

                for (int i = 0; i < annotation.properties().length; i += 2) {
                    fieldProperties.put(annotation.properties()[i], annotation.properties()[i + 1]);
                }

                fieldConnection = (RabbitMQConnectionImpl) containerContext.connection().withProperties(fieldProperties);
                ((RabbitMQContext) containerContext).pool().add(fieldConnection);
            }

            field.setAccessible(true);
            field.set(testClassInstance, fieldConnection);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(String.format("Field '%s' annotated with @%s can't set rabbitmq connection",
                    field.getName(), getConnectionAnnotation().getSimpleName()), e);
        }
    }

    @Override
    protected ContainerContext<RabbitMQConnection> createContainerContext(GenericContainer container) {
        return new RabbitMQContext(container);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        super.beforeAll(context);

        var metadata = getMetadata(context);
        if (metadata.runMode() == ContainerMode.PER_RUN) {
            applyTopology(context, metadata.reset() != Topology.Mode.NONE);
        } else if (metadata.runMode() == ContainerMode.PER_CLASS) {
            applyTopology(context, false);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        super.beforeEach(context);

        var metadata = getMetadata(context);
        if (metadata.runMode() == ContainerMode.PER_METHOD) {
            applyTopology(context, false);
        } else if (metadata.reset() == Topology.Mode.PER_METHOD) {
            applyTopology(context, true);
        }
    }

    private void applyTopology(ExtensionContext context, boolean reset) {
        var metadata = getMetadata(context);
        if (metadata.topology().isEmpty()) {
            return;
        }

        ContainerContext<RabbitMQConnection> containerContext = getContainerContext(context);
        if (containerContext == null) {
            return;
        }

        if (reset) {
            containerContext.connection().resetTopology(metadata.topology());
        } else {
            containerContext.connection().declareTopology(metadata.topology());
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var metadata = getMetadata(context);
        var containerContext = getContainerContext(context);
        if (metadata.runMode() != ContainerMode.PER_METHOD && containerContext != null) {
            ((RabbitMQContext) containerContext).pool().clear();
        }

        super.afterEach(context);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context)
            throws ParameterResolutionException {
        final RabbitMQConnection connection = (RabbitMQConnection) super.resolveParameter(parameterContext, context);
        if (connection == null) {
            return null;
        }

        final ConnectionRabbitMQ annotation = parameterContext.getParameter().getAnnotation(ConnectionRabbitMQ.class);
        if (annotation.properties().length == 0) {
            return connection;
        }

        if (annotation.properties().length % 2 != 0) {
            throw new ExtensionConfigurationException(
                    "@ConnectionRabbitMQ#properties must have even number, properties expected as map of keys and values");
        }

        var properties = connection.params().properties();
        for (int i = 0; i < annotation.properties().length; i += 2) {
            properties.put(annotation.properties()[i], annotation.properties()[i + 1]);
        }

        var extensionContainer = getContainerContext(context);
        var paramConnection = connection.withProperties(properties);
        ((RabbitMQContext) extensionContainer).pool().add((RabbitMQConnectionImpl) paramConnection);
        return paramConnection;
    }

    private static RabbitMQConnection.TopologySpec toTopology(Topology topology) {
        return new RabbitMQConnection.TopologySpec(
                Arrays.stream(topology.queues())
                        .map(TestcontainersRabbitMQExtension::toQueue)
                        .collect(Collectors.toSet()),
                Arrays.stream(topology.exchanges())
                        .map(TestcontainersRabbitMQExtension::toExchange)
                        .collect(Collectors.toSet()),
                Arrays.stream(topology.bindings())
                        .map(TestcontainersRabbitMQExtension::toBinding)
                        .collect(Collectors.toSet()));
    }

    private static RabbitMQConnection.QueueSpec toQueue(Queue queue) {
        return new RabbitMQConnection.QueueSpec(queue.name(), queue.durable(), queue.exclusive(), queue.autoDelete(),
                toArguments(queue.arguments(), "Queue"));
    }

    private static RabbitMQConnection.ExchangeSpec toExchange(Exchange exchange) {
        return new RabbitMQConnection.ExchangeSpec(exchange.name(), exchange.type().name().toLowerCase(Locale.ROOT),
                exchange.durable(), exchange.autoDelete(), exchange.internal(),
                toArguments(exchange.arguments(), "Exchange"));
    }

    private static RabbitMQConnection.BindingSpec toBinding(Binding binding) {
        return new RabbitMQConnection.BindingSpec(binding.queue(), binding.exchange(), binding.routingKey(),
                toArguments(binding.arguments(), "Binding"));
    }

    private static Map<String, Object> toArguments(String[] arguments, String source) {
        if (arguments.length == 0) {
            return Collections.emptyMap();
        }

        if (arguments.length % 2 != 0) {
            throw new ExtensionConfigurationException(
                    "@" + source + "#arguments must have even number, arguments expected as map of keys and values");
        }

        final Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < arguments.length; i += 2) {
            map.put(arguments[i], arguments[i + 1]);
        }
        return map;
    }
}