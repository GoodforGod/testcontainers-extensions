package io.goodforgod.testcontainers.extensions.rabbitmq;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.TestcontainersProvider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.testcontainers.containers.GenericContainer;

@Internal
public final class RabbitMQTestcontainersProvider
        implements
        TestcontainersProvider<TestcontainersRabbitMQ, RabbitMQConnection> {

    private final TestcontainersRabbitMQExtension delegate = new TestcontainersRabbitMQExtension();

    @Override
    public @NotNull Class<TestcontainersRabbitMQ> annotationType() {
        return TestcontainersRabbitMQ.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerRabbitMQ.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionRabbitMQ.class;
    }

    @Override
    public @NotNull Class<RabbitMQConnection> connectionType() {
        return RabbitMQConnection.class;
    }

    @Override
    public @NotNull ContainerMode mode(@NotNull TestcontainersRabbitMQ annotation) {
        return annotation.mode();
    }

    @Override
    public @NotNull String image(@NotNull TestcontainersRabbitMQ annotation) {
        return annotation.image();
    }

    @Override
    public boolean networkShared(@NotNull TestcontainersRabbitMQ annotation) {
        return annotation.network().shared();
    }

    @Override
    public String networkAlias(@NotNull TestcontainersRabbitMQ annotation) {
        return annotation.network().alias();
    }

    @Override
    public @NotNull GenericContainer<?> createContainer(@NotNull TestcontainersRabbitMQ annotation) {
        return delegate.createContainerDefault(metadata(annotation));
    }

    @Override
    public @NotNull ContainerContext<RabbitMQConnection> createContext(@NotNull GenericContainer<?> container) {
        return delegate.createContainerContext(container);
    }

    @Override
    public void afterStart(@NotNull TestcontainersRabbitMQ annotation,
                           @NotNull ContainerContext<RabbitMQConnection> context,
                           @NotNull ExtensionContext extension) {
        if (annotation.mode() == ContainerMode.PER_RUN) {
            applyTopology(annotation, context.connection(), annotation.topology().reset() != Topology.Mode.NONE);
        } else if (annotation.mode() == ContainerMode.PER_CLASS) {
            applyTopology(annotation, context.connection(), false);
        }
    }

    @Override
    public void beforeEach(@NotNull TestcontainersRabbitMQ annotation,
                           @NotNull ContainerContext<RabbitMQConnection> context,
                           @NotNull ExtensionContext extension) {
        if (annotation.mode() == ContainerMode.PER_METHOD) {
            applyTopology(annotation, context.connection(), false);
        } else if (annotation.topology().reset() == Topology.Mode.PER_METHOD) {
            applyTopology(annotation, context.connection(), true);
        }
    }

    @Override
    public void afterEach(@NotNull TestcontainersRabbitMQ annotation,
                          @NotNull ContainerContext<RabbitMQConnection> context,
                          @NotNull ExtensionContext extension) {
        if (annotation.mode() != ContainerMode.PER_METHOD) {
            ((RabbitMQContext) context).pool().clear();
        }
    }

    @Override
    public Object resolveParameter(@NotNull ContainerContext<RabbitMQConnection> context,
                                   @NotNull ParameterContext parameter) {
        ConnectionRabbitMQ annotation = parameter.getParameter().getAnnotation(ConnectionRabbitMQ.class);
        return withProperties(context.connection(), annotation.properties(), (RabbitMQContext) context);
    }

    @Override
    public void injectField(@NotNull ContainerContext<RabbitMQConnection> context,
                            @NotNull Field field,
                            @NotNull Object instance) {
        ConnectionRabbitMQ annotation = field.getAnnotation(ConnectionRabbitMQ.class);
        Object value = withProperties(context.connection(), annotation.properties(), (RabbitMQContext) context);
        try {
            field.setAccessible(true);
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(String.format("Field '%s' annotated with @%s can't set rabbitmq connection",
                    field.getName(), ConnectionRabbitMQ.class.getSimpleName()), e);
        }
    }

    private static RabbitMQMetadata metadata(TestcontainersRabbitMQ annotation) {
        return new RabbitMQMetadata(annotation.network().shared(), annotation.network().alias(), annotation.image(),
                annotation.mode(), toTopology(annotation.topology()), annotation.topology().reset());
    }

    private static void applyTopology(TestcontainersRabbitMQ annotation, RabbitMQConnection connection, boolean reset) {
        RabbitMQConnection.TopologySpec topology = toTopology(annotation.topology());
        if (topology.isEmpty()) {
            return;
        }

        if (reset) {
            connection.resetTopology(topology);
        } else {
            connection.declareTopology(topology);
        }
    }

    private static RabbitMQConnection withProperties(RabbitMQConnection connection,
                                                     String[] properties,
                                                     RabbitMQContext context) {
        if (properties.length == 0) {
            return connection;
        }

        if (properties.length % 2 != 0) {
            throw new ExtensionConfigurationException(
                    "@ConnectionRabbitMQ#properties must have even number, properties expected as map of keys and values");
        }

        Properties props = connection.params().properties();
        for (int i = 0; i < properties.length; i += 2) {
            props.put(properties[i], properties[i + 1]);
        }

        RabbitMQConnection paramConnection = connection.withProperties(props);
        context.pool().add((RabbitMQConnectionImpl) paramConnection);
        return paramConnection;
    }

    private static RabbitMQConnection.TopologySpec toTopology(Topology topology) {
        return new RabbitMQConnection.TopologySpec(
                Arrays.stream(topology.queues())
                        .map(RabbitMQTestcontainersProvider::toQueue)
                        .collect(Collectors.toSet()),
                Arrays.stream(topology.exchanges())
                        .map(RabbitMQTestcontainersProvider::toExchange)
                        .collect(Collectors.toSet()),
                Arrays.stream(topology.bindings())
                        .map(RabbitMQTestcontainersProvider::toBinding)
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

        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < arguments.length; i += 2) {
            map.put(arguments[i], arguments[i + 1]);
        }
        return map;
    }
}
