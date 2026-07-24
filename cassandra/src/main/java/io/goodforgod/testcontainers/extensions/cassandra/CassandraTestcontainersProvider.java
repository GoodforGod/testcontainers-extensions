package io.goodforgod.testcontainers.extensions.cassandra;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.TestcontainersProvider;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.containers.GenericContainer;

@Internal
public final class CassandraTestcontainersProvider
        implements
        TestcontainersProvider<TestcontainersCassandra, CassandraConnection> {

    private final TestcontainersCassandraExtension delegate = new TestcontainersCassandraExtension();

    @Override
    public @NotNull Class<TestcontainersCassandra> annotationType() {
        return TestcontainersCassandra.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerCassandra.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionCassandra.class;
    }

    @Override
    public @NotNull Class<CassandraConnection> connectionType() {
        return CassandraConnection.class;
    }

    @Override
    public @NotNull ContainerMode mode(@NotNull TestcontainersCassandra annotation) {
        return annotation.mode();
    }

    @Override
    public @NotNull String image(@NotNull TestcontainersCassandra annotation) {
        return annotation.image();
    }

    @Override
    public boolean networkShared(@NotNull TestcontainersCassandra annotation) {
        return annotation.network().shared();
    }

    @Override
    public String networkAlias(@NotNull TestcontainersCassandra annotation) {
        return annotation.network().alias();
    }

    @Override
    public @NotNull GenericContainer<?> createContainer(@NotNull TestcontainersCassandra annotation) {
        validate(annotation);
        var metadata = new CassandraMetadata(annotation.network().shared(), annotation.network().alias(),
                annotation.image(), annotation.mode(), annotation.migration());
        return delegate.createContainerDefault(metadata);
    }

    @Override
    public @NotNull ContainerContext<CassandraConnection> createContext(@NotNull GenericContainer<?> container) {
        return delegate.createContainerContext((CassandraContainer) container);
    }

    @Override
    public void beforeEach(@NotNull TestcontainersCassandra annotation,
                           @NotNull ContainerContext<CassandraConnection> context,
                           @NotNull ExtensionContext extension) {
        if (annotation.migration().apply() == Migration.Mode.PER_METHOD) {
            migrate(annotation, context.connection());
        }
    }

    @Override
    public void afterStart(@NotNull TestcontainersCassandra annotation,
                           @NotNull ContainerContext<CassandraConnection> context,
                           @NotNull ExtensionContext extension) {
        if (annotation.migration().apply() == Migration.Mode.PER_CLASS) {
            migrate(annotation, context.connection());
        }
    }

    @Override
    public void afterEach(@NotNull TestcontainersCassandra annotation,
                          @NotNull ContainerContext<CassandraConnection> context,
                          @NotNull ExtensionContext extension) {
        if (annotation.migration().drop() == Migration.Mode.PER_METHOD && annotation.mode() != ContainerMode.PER_METHOD) {
            drop(annotation, context.connection());
        }
    }

    @Override
    public void beforeStop(@NotNull TestcontainersCassandra annotation,
                           @NotNull ContainerContext<CassandraConnection> context,
                           @NotNull ExtensionContext extension) {
        if (annotation.migration().drop() == Migration.Mode.PER_CLASS && annotation.mode() == ContainerMode.PER_RUN) {
            drop(annotation, context.connection());
        }
    }

    private static void validate(TestcontainersCassandra annotation) {
        if (annotation.mode() == ContainerMode.PER_METHOD && annotation.migration().apply() == Migration.Mode.PER_CLASS) {
            throw new ExtensionConfigurationException(String.format(
                    "@%s can't apply migration in Migration.Mode.PER_CLASS mode when ContainerMode.PER_METHOD is used",
                    ContainerCassandra.class.getSimpleName()));
        }
    }

    private static void migrate(TestcontainersCassandra annotation, CassandraConnection connection) {
        CassandraMigrationEngine migrationEngine = connection.migrationEngine(annotation.migration().engine());
        migrationEngine.apply(Arrays.asList(annotation.migration().locations()));
    }

    private static void drop(TestcontainersCassandra annotation, CassandraConnection connection) {
        CassandraMigrationEngine migrationEngine = connection.migrationEngine(annotation.migration().engine());
        migrationEngine.drop(Arrays.asList(annotation.migration().locations()), annotation.migration().dropMode());
    }
}
