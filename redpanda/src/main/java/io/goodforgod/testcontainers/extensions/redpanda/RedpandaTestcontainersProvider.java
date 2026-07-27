package io.goodforgod.testcontainers.extensions.redpanda;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.TestcontainersProvider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.Set;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.redpanda.RedpandaContainer;

@Internal
public final class RedpandaTestcontainersProvider
        implements
        TestcontainersProvider<TestcontainersRedpanda, RedpandaConnection> {

    private final TestcontainersRedpandaExtension delegate = new TestcontainersRedpandaExtension();

    @Override
    public @NotNull Class<TestcontainersRedpanda> annotationType() {
        return TestcontainersRedpanda.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerRedpanda.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionRedpanda.class;
    }

    @Override
    public @NotNull Class<RedpandaConnection> connectionType() {
        return RedpandaConnection.class;
    }

    @Override
    public @NotNull ContainerMode mode(@NotNull TestcontainersRedpanda annotation) {
        return annotation.mode();
    }

    @Override
    public @NotNull String image(@NotNull TestcontainersRedpanda annotation) {
        return annotation.image();
    }

    @Override
    public boolean networkShared(@NotNull TestcontainersRedpanda annotation) {
        return annotation.network().shared();
    }

    @Override
    public String networkAlias(@NotNull TestcontainersRedpanda annotation) {
        return annotation.network().alias();
    }

    @Override
    public @NotNull GenericContainer<?> createContainer(@NotNull TestcontainersRedpanda annotation) {
        validate(annotation);
        return delegate.createContainerDefault(metadata(annotation));
    }

    @Override
    public @NotNull ContainerContext<RedpandaConnection> createContext(@NotNull GenericContainer<?> container) {
        return delegate.createContainerContext((RedpandaContainer) container);
    }

    @Override
    public void afterStart(@NotNull TestcontainersRedpanda annotation,
                           @NotNull ContainerContext<RedpandaConnection> context,
                           @NotNull ExtensionContext extension) {
        if (topics(annotation).isEmpty()) {
            return;
        }

        RedpandaConnection connection = context.connection();
        if (annotation.mode() == ContainerMode.PER_RUN) {
            connection.createTopics(topics(annotation));
            ((RedpandaConnectionImpl) connection).createTopicsIfNeeded(topics(annotation),
                    annotation.topics().reset() != Topics.Mode.NONE);
        } else if (annotation.mode() == ContainerMode.PER_CLASS) {
            ((RedpandaConnectionImpl) connection).createTopicsIfNeeded(topics(annotation), false);
        }
    }

    @Override
    public void beforeEach(@NotNull TestcontainersRedpanda annotation,
                           @NotNull ContainerContext<RedpandaConnection> context,
                           @NotNull ExtensionContext extension) {
        if (topics(annotation).isEmpty()) {
            return;
        }

        RedpandaConnection connection = context.connection();
        if (annotation.mode() == ContainerMode.PER_METHOD) {
            ((RedpandaConnectionImpl) connection).createTopicsIfNeeded(topics(annotation), false);
        } else if (annotation.topics().reset() == Topics.Mode.PER_METHOD) {
            ((RedpandaConnectionImpl) connection).createTopicsIfNeeded(topics(annotation), true);
        }
    }

    @Override
    public void afterEach(@NotNull TestcontainersRedpanda annotation,
                          @NotNull ContainerContext<RedpandaConnection> context,
                          @NotNull ExtensionContext extension) {
        if (annotation.mode() != ContainerMode.PER_METHOD) {
            ((RedpandaContext) context).pool().clear();
        }
    }

    @Override
    public Object resolveParameter(@NotNull ContainerContext<RedpandaConnection> context,
                                   @NotNull ParameterContext parameter) {
        ConnectionRedpanda annotation = parameter.getParameter().getAnnotation(ConnectionRedpanda.class);
        return withProperties(context.connection(), annotation.properties(), (RedpandaContext) context);
    }

    @Override
    public void injectField(@NotNull ContainerContext<RedpandaConnection> context,
                            @NotNull Field field,
                            @NotNull Object instance) {
        ConnectionRedpanda annotation = field.getAnnotation(ConnectionRedpanda.class);
        Object value = withProperties(context.connection(), annotation.properties(), (RedpandaContext) context);
        try {
            field.setAccessible(true);
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(String.format("Field '%s' annotated with @%s can't set RedpandaConnection",
                    field.getName(), ConnectionRedpanda.class.getSimpleName()), e);
        }
    }

    private static RedpandaMetadata metadata(TestcontainersRedpanda annotation) {
        return new RedpandaMetadata(annotation.network().shared(), annotation.network().alias(), annotation.image(),
                annotation.mode(), topics(annotation), annotation.topics().reset());
    }

    private static Set<String> topics(TestcontainersRedpanda annotation) {
        return Set.of(annotation.topics().value());
    }

    private static void validate(TestcontainersRedpanda annotation) {
        if (annotation.mode() == ContainerMode.PER_METHOD && annotation.topics().reset() == Topics.Mode.PER_CLASS) {
            throw new ExtensionConfigurationException(
                    String.format("@%s can't apply migration in Topics.Mode.PER_CLASS mode when ContainerMode.PER_METHOD is used",
                            ContainerRedpanda.class.getSimpleName()));
        }
    }

    private static RedpandaConnection withProperties(RedpandaConnection connection,
                                                     String[] properties,
                                                     RedpandaContext context) {
        if (properties.length == 0) {
            return connection;
        }

        if (properties.length % 2 != 0) {
            throw new ExtensionConfigurationException(
                    "@ConnectionRedpanda#properties must have even number, properties expected as map of keys and values");
        }

        Properties props = connection.params().properties();
        for (int i = 0; i < properties.length; i += 2) {
            props.put(properties[i], properties[i + 1]);
        }

        RedpandaConnection paramConnection = connection.withProperties(props);
        context.pool().add((RedpandaConnectionImpl) paramConnection);
        return paramConnection;
    }
}
