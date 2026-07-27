package io.goodforgod.testcontainers.extensions.scylla;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.Isolation;
import io.goodforgod.testcontainers.extensions.TestcontainersProvider;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.scylladb.ScyllaDBContainer;

@Internal
public final class ScyllaTestcontainersProvider implements TestcontainersProvider<TestcontainersScylla, ScyllaConnection> {

    private final TestcontainersScyllaExtension delegate = new TestcontainersScyllaExtension();

    @Override
    public @NotNull Class<TestcontainersScylla> annotationType() {
        return TestcontainersScylla.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerScylla.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionScylla.class;
    }

    @Override
    public @NotNull Class<ScyllaConnection> connectionType() {
        return ScyllaConnection.class;
    }

    @Override
    public @NotNull ContainerMode mode(@NotNull TestcontainersScylla annotation) {
        return annotation.mode();
    }

    @Override
    public @NotNull String image(@NotNull TestcontainersScylla annotation) {
        return annotation.image();
    }

    @Override
    public boolean networkShared(@NotNull TestcontainersScylla annotation) {
        return annotation.network().shared();
    }

    @Override
    public String networkAlias(@NotNull TestcontainersScylla annotation) {
        return annotation.network().alias();
    }

    @Override
    public @NotNull GenericContainer<?> createContainer(@NotNull TestcontainersScylla annotation) {
        validate(annotation);
        var metadata = new ScyllaMetadata(annotation.network().shared(), annotation.network().alias(),
                annotation.image(), annotation.mode(), annotation.migration());
        return delegate.createContainerDefault(metadata);
    }

    @Override
    public @NotNull ContainerContext<ScyllaConnection> createContext(@NotNull GenericContainer<?> container) {
        return delegate.createContainerContext((ScyllaDBContainer) container);
    }

    @Override
    public String isolationPrefix(@NotNull TestcontainersScylla annotation) {
        return "scylla";
    }

    @Override
    public @NotNull ScyllaConnection createIsolatedConnection(@NotNull TestcontainersScylla annotation,
                                                              @NotNull ContainerContext<ScyllaConnection> context,
                                                              @NotNull ExtensionContext extension,
                                                              @NotNull String namespace) {
        ScyllaConnection connection = context.connection();
        ScyllaConnection.Params params = connection.params();
        ScyllaConnection.Params network = connection.paramsInNetwork().orElse(null);
        return new ScyllaConnectionClosableImpl(
                new ScyllaConnectionImpl.ParamsImpl(params.host(), params.port(), params.datacenter(), namespace,
                        params.username(), params.password()),
                (network == null)
                        ? null
                        : new ScyllaConnectionImpl.ParamsImpl(network.host(), network.port(), network.datacenter(), namespace,
                                network.username(), network.password()));
    }

    @Override
    public void closeIsolatedConnection(@NotNull TestcontainersScylla annotation,
                                        @NotNull ScyllaConnection connection,
                                        @NotNull ExtensionContext extension) {
        if (connection instanceof ScyllaConnectionImpl scyllaConnection) {
            scyllaConnection.stop();
        }
    }

    @Override
    public void afterStart(@NotNull TestcontainersScylla annotation,
                           @NotNull ContainerContext<ScyllaConnection> context,
                           @NotNull ExtensionContext extension) {
        if (isolation(annotation) != Isolation.Mode.DISABLED) {
            return;
        }

        if (annotation.migration().apply() == Migration.Mode.PER_CLASS) {
            migrate(annotation, context.connection());
        }
    }

    @Override
    public void beforeEach(@NotNull TestcontainersScylla annotation,
                           @NotNull ContainerContext<ScyllaConnection> context,
                           @NotNull ExtensionContext extension) {
        if (isolation(annotation) != Isolation.Mode.DISABLED && annotation.migration().apply() != Migration.Mode.NONE) {
            migrate(annotation, context.connection());
        } else if (annotation.migration().apply() == Migration.Mode.PER_METHOD) {
            migrate(annotation, context.connection());
        }
    }

    @Override
    public void afterEach(@NotNull TestcontainersScylla annotation,
                          @NotNull ContainerContext<ScyllaConnection> context,
                          @NotNull ExtensionContext extension) {
        if (annotation.migration().drop() == Migration.Mode.PER_METHOD && annotation.mode() != ContainerMode.PER_METHOD) {
            drop(annotation, context.connection());
        }
    }

    @Override
    public void beforeStop(@NotNull TestcontainersScylla annotation,
                           @NotNull ContainerContext<ScyllaConnection> context,
                           @NotNull ExtensionContext extension) {
        if (annotation.migration().drop() == Migration.Mode.PER_CLASS && annotation.mode() == ContainerMode.PER_RUN) {
            drop(annotation, context.connection());
        }
    }

    private static void validate(TestcontainersScylla annotation) {
        if (annotation.mode() == ContainerMode.PER_METHOD && annotation.migration().apply() == Migration.Mode.PER_CLASS) {
            throw new ExtensionConfigurationException(String.format(
                    "@%s can't apply migration in Migration.Mode.PER_CLASS mode when ContainerMode.PER_METHOD is used",
                    ContainerScylla.class.getSimpleName()));
        }
    }

    private static void migrate(TestcontainersScylla annotation, ScyllaConnection connection) {
        ScyllaMigrationEngine migrationEngine = connection.migrationEngine(annotation.migration().engine());
        migrationEngine.apply(Arrays.asList(annotation.migration().locations()));
    }

    private static void drop(TestcontainersScylla annotation, ScyllaConnection connection) {
        ScyllaMigrationEngine migrationEngine = connection.migrationEngine(annotation.migration().engine());
        migrationEngine.drop(Arrays.asList(annotation.migration().locations()), annotation.migration().dropMode());
    }
}
