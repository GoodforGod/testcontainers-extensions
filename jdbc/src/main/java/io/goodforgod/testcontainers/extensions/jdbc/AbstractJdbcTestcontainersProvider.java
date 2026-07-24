package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.Isolation;
import io.goodforgod.testcontainers.extensions.Network;
import io.goodforgod.testcontainers.extensions.TestcontainersProvider;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;

public abstract class AbstractJdbcTestcontainersProvider<A extends Annotation, T extends JdbcDatabaseContainer<?>>
        implements
        TestcontainersProvider<A, JdbcConnection> {

    private record MigrationTemplateKey(Class<?> provider, String image, Migration.Engines engine, List<String> locations) {}

    protected abstract AbstractTestcontainersJdbcExtension<T, JdbcMetadata> delegate();

    protected abstract JdbcMetadata metadata(@NotNull A annotation);

    protected abstract Migration migration(@NotNull A annotation);

    protected abstract Isolation isolationAnnotation(@NotNull A annotation);

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
    public Isolation.Mode isolation(@NotNull A annotation) {
        return isolationAnnotation(annotation).value();
    }

    @Override
    public String isolationPrefix(@NotNull A annotation) {
        return metadata(annotation).image()
                .replaceAll("[:/].*", "")
                .replaceAll("[^A-Za-z0-9_]", "_");
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
    public @NotNull JdbcConnection createIsolatedConnection(@NotNull A annotation,
                                                            @NotNull ContainerContext<JdbcConnection> context,
                                                            @NotNull ExtensionContext extension,
                                                            @NotNull String namespace) {
        JdbcConnection connection = context.connection();
        if (migration(annotation).strategy() == Migration.Strategy.TEMPLATE_CLONE
                && migration(annotation).apply() != Migration.Mode.NONE) {
            cloneTemplateDatabase(annotation, connection, extension, namespace);
        } else {
            createDatabaseIfNeeded(connection, namespace);
        }

        return connectionForDatabase(connection, namespace);
    }

    @Override
    public void closeIsolatedConnection(@NotNull A annotation,
                                        @NotNull JdbcConnection connection,
                                        @NotNull ExtensionContext extension) {
        if (connection instanceof JdbcConnectionImpl jdbcConnection) {
            jdbcConnection.stop();
        }
    }

    @Override
    public void afterStart(@NotNull A annotation,
                           @NotNull ContainerContext<JdbcConnection> context,
                           @NotNull ExtensionContext extension) {
        if (isolation(annotation) != Isolation.Mode.DISABLED) {
            return;
        }

        if (migration(annotation).apply() == Migration.Mode.PER_CLASS) {
            migrate(annotation, context.connection());
        }
    }

    @Override
    public void beforeEach(@NotNull A annotation,
                           @NotNull ContainerContext<JdbcConnection> context,
                           @NotNull ExtensionContext extension) {
        if (isolation(annotation) != Isolation.Mode.DISABLED
                && migration(annotation).apply() != Migration.Mode.NONE
                && migration(annotation).strategy() != Migration.Strategy.TEMPLATE_CLONE) {
            migrate(annotation, context.connection());
        } else if (migration(annotation).apply() == Migration.Mode.PER_METHOD) {
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
        if (isolation(annotation) != Isolation.Mode.DISABLED && !isIsolationSupported(annotation)) {
            throw new ExtensionConfigurationException("@%s doesn't support Isolation.Mode.PER_METHOD"
                    .formatted(containerAnnotationType().getSimpleName()));
        }

        if (migration(annotation).strategy() == Migration.Strategy.TEMPLATE_CLONE) {
            if (isolation(annotation) == Isolation.Mode.DISABLED) {
                throw new ExtensionConfigurationException(
                        "@%s Migration.Strategy.TEMPLATE_CLONE requires Isolation.Mode.PER_METHOD"
                                .formatted(containerAnnotationType().getSimpleName()));
            }

            if (!isTemplateCloneSupported(annotation)) {
                throw new ExtensionConfigurationException("@%s doesn't support Migration.Strategy.TEMPLATE_CLONE"
                        .formatted(containerAnnotationType().getSimpleName()));
            }
        }

        if (mode(annotation) == ContainerMode.PER_METHOD && migration(annotation).apply() == Migration.Mode.PER_CLASS) {
            throw new ExtensionConfigurationException(String.format(
                    "@%s can't apply migration in Migration.Mode.PER_CLASS mode when ContainerMode.PER_METHOD is used",
                    containerAnnotationType().getSimpleName()));
        }
    }

    protected boolean isIsolationSupported(A annotation) {
        return true;
    }

    protected boolean isTemplateCloneSupported(A annotation) {
        return false;
    }

    protected String createDatabaseSql(String namespace) {
        return "CREATE DATABASE " + namespace;
    }

    protected String createDatabaseFromTemplateSql(String namespace, String template) {
        return "CREATE DATABASE " + namespace + " TEMPLATE " + template;
    }

    protected boolean databaseExists(JdbcConnection connection, String database) {
        return false;
    }

    private void cloneTemplateDatabase(A annotation,
                                       JdbcConnection connection,
                                       ExtensionContext extension,
                                       String namespace) {
        String template = migrationTemplate(extension, annotation, connection);
        connection.execute(createDatabaseFromTemplateSql(namespace, template));
    }

    private String migrationTemplate(ExtensionContext extension, A annotation, JdbcConnection connection) {
        MigrationTemplateKey key = new MigrationTemplateKey(providerClass(), image(annotation), migration(annotation).engine(),
                List.of(migration(annotation).locations()));
        return classStore(extension).getOrComputeIfAbsent(key, ignored -> {
            String template = "migration_template_" + Integer.toUnsignedString(Math.abs(Objects.hash(key)), 36)
                    .toLowerCase(Locale.ROOT);
            synchronized (providerClass()) {
                createDatabaseIfNeeded(connection, template);
                JdbcConnection templateConnection = connectionForDatabase(connection, template);
                try {
                    migrate(annotation, templateConnection);
                } finally {
                    closeIsolatedConnection(annotation, templateConnection, extension);
                }
            }

            return template;
        }, String.class);
    }

    private void createDatabaseIfNeeded(JdbcConnection connection, String database) {
        if (!databaseExists(connection, database)) {
            connection.execute(createDatabaseSql(database));
        }
    }

    private JdbcConnection connectionForDatabase(JdbcConnection connection, String database) {
        JdbcConnection.Params params = connection.params();
        JdbcConnection.Params network = connection.paramsInNetwork().orElse(null);
        return new JdbcConnectionClosableImpl(
                new JdbcConnectionImpl.ParamsImpl(replaceDatabase(params.jdbcUrl(), params.database(), database),
                        params.host(),
                        params.port(),
                        database,
                        params.username(),
                        params.password()),
                (network == null)
                        ? null
                        : new JdbcConnectionImpl.ParamsImpl(replaceDatabase(network.jdbcUrl(), network.database(), database),
                                network.host(),
                                network.port(),
                                database,
                                network.username(),
                                network.password()));
    }

    private ExtensionContext.Store classStore(ExtensionContext context) {
        return (context.getParent().isPresent() && context.getParent().get().getParent().isPresent())
                ? context.getParent().get().getStore(ExtensionContext.Namespace.create(providerClass()))
                : context.getStore(ExtensionContext.Namespace.create(providerClass()));
    }

    private Class<?> providerClass() {
        return getClass();
    }

    private static String replaceDatabase(String jdbcUrl, String database, String namespace) {
        String normalizedDatabase = database.startsWith("/")
                ? database.substring(1)
                : database;
        int idx = jdbcUrl.lastIndexOf('/' + normalizedDatabase);
        if (idx < 0) {
            return jdbcUrl;
        }

        return jdbcUrl.substring(0, idx + 1) + namespace + jdbcUrl.substring(idx + normalizedDatabase.length() + 1);
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
