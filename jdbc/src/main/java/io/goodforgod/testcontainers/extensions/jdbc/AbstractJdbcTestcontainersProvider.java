package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.Network;
import io.goodforgod.testcontainers.extensions.TestcontainersProvider;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;

public abstract class AbstractJdbcTestcontainersProvider<A extends Annotation, T extends JdbcDatabaseContainer<?>>
        implements
        TestcontainersProvider<A, JdbcConnection> {

    protected abstract AbstractTestcontainersJdbcExtension<T, JdbcMetadata> delegate();

    protected abstract JdbcMetadata metadata(@NotNull A annotation);

    protected abstract Migration migration(@NotNull A annotation);

    @Override
    public @NotNull Class<JdbcConnection> connectionType() {
        return JdbcConnection.class;
    }

    @Override
    public @NotNull ContainerMode mode(@NotNull A annotation) {
        return metadata(annotation).runMode();
    }

    @Override
    public @NotNull String image(@NotNull A annotation) {
        return metadata(annotation).image();
    }

    @Override
    public boolean networkShared(@NotNull A annotation) {
        return metadata(annotation).networkShared();
    }

    @Override
    public String networkAlias(@NotNull A annotation) {
        return metadata(annotation).networkAlias();
    }

    @Override
    public @NotNull GenericContainer<?> createContainer(@NotNull A annotation) {
        validate(annotation);
        return delegate().createContainerFromProvider(metadata(annotation));
    }

    @Override
    public @NotNull ContainerContext<JdbcConnection> createContext(@NotNull GenericContainer<?> container) {
        return delegate().createContextFromProvider((T) container);
    }

    @Override
    public void afterStart(@NotNull A annotation,
                           @NotNull ContainerContext<JdbcConnection> context,
                           @NotNull ExtensionContext extension) {
        if (migration(annotation).apply() == Migration.Mode.PER_CLASS) {
            migrate(annotation, context.connection());
        }
    }

    @Override
    public void beforeEach(@NotNull A annotation,
                           @NotNull ContainerContext<JdbcConnection> context,
                           @NotNull ExtensionContext extension) {
        if (migration(annotation).apply() == Migration.Mode.PER_METHOD) {
            migrate(annotation, context.connection());
        }
    }

    @Override
    public void afterEach(@NotNull A annotation,
                          @NotNull ContainerContext<JdbcConnection> context,
                          @NotNull ExtensionContext extension) {
        if (migration(annotation).drop() == Migration.Mode.PER_METHOD && mode(annotation) != ContainerMode.PER_METHOD) {
            drop(annotation, context.connection());
        }
    }

    @Override
    public void beforeStop(@NotNull A annotation,
                           @NotNull ContainerContext<JdbcConnection> context,
                           @NotNull ExtensionContext extension) {
        if (migration(annotation).drop() == Migration.Mode.PER_CLASS && mode(annotation) == ContainerMode.PER_RUN) {
            drop(annotation, context.connection());
        }
    }

    protected static JdbcMetadata metadata(Network network,
                                           String image,
                                           ContainerMode mode,
                                           Migration migration) {
        return new JdbcMetadata(network.shared(), network.alias(), image, mode, migration);
    }

    private void validate(A annotation) {
        if (mode(annotation) == ContainerMode.PER_METHOD && migration(annotation).apply() == Migration.Mode.PER_CLASS) {
            throw new ExtensionConfigurationException(String.format(
                    "@%s can't apply migration in Migration.Mode.PER_CLASS mode when ContainerMode.PER_METHOD is used",
                    containerAnnotationType().getSimpleName()));
        }
    }

    private void migrate(A annotation, JdbcConnection connection) {
        JdbcMigrationEngine migrationEngine = connection.migrationEngine(migration(annotation).engine());
        migrationEngine.apply(Arrays.asList(migration(annotation).locations()));
    }

    private void drop(A annotation, JdbcConnection connection) {
        JdbcMigrationEngine migrationEngine = connection.migrationEngine(migration(annotation).engine());
        migrationEngine.drop(Arrays.asList(migration(annotation).locations()));
    }
}
