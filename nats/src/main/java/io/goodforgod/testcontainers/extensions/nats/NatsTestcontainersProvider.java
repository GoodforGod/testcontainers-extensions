package io.goodforgod.testcontainers.extensions.nats;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.TestcontainersProvider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Properties;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.testcontainers.containers.GenericContainer;

@Internal
public final class NatsTestcontainersProvider implements TestcontainersProvider<TestcontainersNats, NatsConnection> {

    private final TestcontainersNatsExtension delegate = new TestcontainersNatsExtension();

    @Override
    public @NotNull Class<TestcontainersNats> annotationType() {
        return TestcontainersNats.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerNats.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionNats.class;
    }

    @Override
    public @NotNull Class<NatsConnection> connectionType() {
        return NatsConnection.class;
    }

    @Override
    public @NotNull ContainerMode mode(@NotNull TestcontainersNats annotation) {
        return annotation.mode();
    }

    @Override
    public @NotNull String image(@NotNull TestcontainersNats annotation) {
        return annotation.image();
    }

    @Override
    public boolean networkShared(@NotNull TestcontainersNats annotation) {
        return annotation.network().shared();
    }

    @Override
    public String networkAlias(@NotNull TestcontainersNats annotation) {
        return annotation.network().alias();
    }

    @Override
    public @NotNull GenericContainer<?> createContainer(@NotNull TestcontainersNats annotation) {
        var metadata = new NatsMetadata(annotation.network().shared(), annotation.network().alias(),
                annotation.image(), annotation.mode());
        return delegate.createContainerDefault(metadata);
    }

    @Override
    public @NotNull ContainerContext<NatsConnection> createContext(@NotNull GenericContainer<?> container) {
        return delegate.createContainerContext(container);
    }

    @Override
    public void afterEach(@NotNull TestcontainersNats annotation,
                          @NotNull ContainerContext<NatsConnection> context,
                          @NotNull ExtensionContext extension) {
        if (annotation.mode() != ContainerMode.PER_METHOD) {
            ((NatsContext) context).pool().clear();
        }
    }

    @Override
    public Object resolveParameter(@NotNull ContainerContext<NatsConnection> context, @NotNull ParameterContext parameter) {
        ConnectionNats annotation = parameter.getParameter().getAnnotation(ConnectionNats.class);
        return withProperties(context.connection(), annotation.properties(), (NatsContext) context);
    }

    @Override
    public void injectField(@NotNull ContainerContext<NatsConnection> context,
                            @NotNull Field field,
                            @NotNull Object instance) {
        ConnectionNats annotation = field.getAnnotation(ConnectionNats.class);
        Object value = withProperties(context.connection(), annotation.properties(), (NatsContext) context);
        try {
            field.setAccessible(true);
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(String.format("Field '%s' annotated with @%s can't set nats connection",
                    field.getName(), ConnectionNats.class.getSimpleName()), e);
        }
    }

    private static NatsConnection withProperties(NatsConnection connection, String[] properties, NatsContext context) {
        if (properties.length == 0) {
            return connection;
        }

        if (properties.length % 2 != 0) {
            throw new ExtensionConfigurationException(
                    "@ConnectionNats#properties must have even number, properties expected as map of keys and values");
        }

        Properties props = connection.params().properties();
        for (int i = 0; i < properties.length; i += 2) {
            props.put(properties[i], properties[i + 1]);
        }

        NatsConnection paramConnection = connection.withProperties(props);
        context.pool().add((NatsConnectionImpl) paramConnection);
        return paramConnection;
    }
}
