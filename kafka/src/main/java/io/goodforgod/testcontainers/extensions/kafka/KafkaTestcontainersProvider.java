package io.goodforgod.testcontainers.extensions.kafka;

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

@Internal
public final class KafkaTestcontainersProvider implements TestcontainersProvider<TestcontainersKafka, KafkaConnection> {

    private final TestcontainersKafkaExtension delegate = new TestcontainersKafkaExtension();

    @Override
    public @NotNull Class<TestcontainersKafka> annotationType() {
        return TestcontainersKafka.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerKafka.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionKafka.class;
    }

    @Override
    public @NotNull Class<KafkaConnection> connectionType() {
        return KafkaConnection.class;
    }

    @Override
    public @NotNull ContainerMode mode(@NotNull TestcontainersKafka annotation) {
        return annotation.mode();
    }

    @Override
    public @NotNull String image(@NotNull TestcontainersKafka annotation) {
        return annotation.image();
    }

    @Override
    public boolean networkShared(@NotNull TestcontainersKafka annotation) {
        return annotation.network().shared();
    }

    @Override
    public String networkAlias(@NotNull TestcontainersKafka annotation) {
        return annotation.network().alias();
    }

    @Override
    public @NotNull GenericContainer<?> createContainer(@NotNull TestcontainersKafka annotation) {
        validate(annotation);
        return delegate.createContainerDefault(metadata(annotation));
    }

    @Override
    public @NotNull ContainerContext<KafkaConnection> createContext(@NotNull GenericContainer<?> container) {
        return delegate.createContainerContext(container);
    }

    @Override
    public void afterStart(@NotNull TestcontainersKafka annotation,
                           @NotNull ContainerContext<KafkaConnection> context,
                           @NotNull ExtensionContext extension) {
        if (topics(annotation).isEmpty()) {
            return;
        }

        KafkaConnection connection = context.connection();
        if (annotation.mode() == ContainerMode.PER_RUN) {
            connection.createTopics(topics(annotation));
            ((KafkaConnectionImpl) connection).createTopicsIfNeeded(topics(annotation),
                    annotation.topics().reset() != Topics.Mode.NONE);
        } else if (annotation.mode() == ContainerMode.PER_CLASS) {
            ((KafkaConnectionImpl) connection).createTopicsIfNeeded(topics(annotation), false);
        }
    }

    @Override
    public void beforeEach(@NotNull TestcontainersKafka annotation,
                           @NotNull ContainerContext<KafkaConnection> context,
                           @NotNull ExtensionContext extension) {
        if (topics(annotation).isEmpty()) {
            return;
        }

        KafkaConnection connection = context.connection();
        if (annotation.mode() == ContainerMode.PER_METHOD) {
            ((KafkaConnectionImpl) connection).createTopicsIfNeeded(topics(annotation), false);
        } else if (annotation.topics().reset() == Topics.Mode.PER_METHOD) {
            ((KafkaConnectionImpl) connection).createTopicsIfNeeded(topics(annotation), true);
        }
    }

    @Override
    public void afterEach(@NotNull TestcontainersKafka annotation,
                          @NotNull ContainerContext<KafkaConnection> context,
                          @NotNull ExtensionContext extension) {
        if (annotation.mode() != ContainerMode.PER_METHOD) {
            ((KafkaContext) context).pool().clear();
        }
    }

    @Override
    public Object resolveParameter(@NotNull ContainerContext<KafkaConnection> context, @NotNull ParameterContext parameter) {
        ConnectionKafka annotation = parameter.getParameter().getAnnotation(ConnectionKafka.class);
        return withProperties(context.connection(), annotation.properties(), (KafkaContext) context);
    }

    @Override
    public void injectField(@NotNull ContainerContext<KafkaConnection> context,
                            @NotNull Field field,
                            @NotNull Object instance) {
        ConnectionKafka annotation = field.getAnnotation(ConnectionKafka.class);
        Object value = withProperties(context.connection(), annotation.properties(), (KafkaContext) context);
        try {
            field.setAccessible(true);
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(String.format("Field '%s' annotated with @%s can't set kafka connection",
                    field.getName(), ConnectionKafka.class.getSimpleName()), e);
        }
    }

    private static KafkaMetadata metadata(TestcontainersKafka annotation) {
        return new KafkaMetadata(annotation.network().shared(), annotation.network().alias(), annotation.image(),
                annotation.mode(), topics(annotation), annotation.topics().reset());
    }

    private static Set<String> topics(TestcontainersKafka annotation) {
        return Set.of(annotation.topics().value());
    }

    private static void validate(TestcontainersKafka annotation) {
        if (annotation.mode() == ContainerMode.PER_METHOD && annotation.topics().reset() == Topics.Mode.PER_CLASS) {
            throw new ExtensionConfigurationException(
                    String.format("@%s can't apply migration in Topics.Mode.PER_CLASS mode when ContainerMode.PER_METHOD is used",
                            ContainerKafka.class.getSimpleName()));
        }
    }

    private static KafkaConnection withProperties(KafkaConnection connection, String[] properties, KafkaContext context) {
        if (properties.length == 0) {
            return connection;
        }

        if (properties.length % 2 != 0) {
            throw new ExtensionConfigurationException(
                    "@ConnectionKafka#properties must have even number, properties expected as map of keys and values");
        }

        Properties props = connection.params().properties();
        for (int i = 0; i < properties.length; i += 2) {
            props.put(properties[i], properties[i + 1]);
        }

        KafkaConnection paramConnection = connection.withProperties(props);
        context.pool().add((KafkaConnectionImpl) paramConnection);
        return paramConnection;
    }
}
