package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.jdbc.example.ContainerJdbc;
import io.goodforgod.testcontainers.extensions.jdbc.example.ContainerJdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.example.TestcontainersJdbc;
import java.lang.annotation.Annotation;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

public final class ExampleTestcontainersJdbcExtension extends
        AbstractTestcontainersJdbcExtension<PostgreSQLContainer<?>, PostgresJdbcMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(ExampleTestcontainersJdbcExtension.class);

    private volatile FlywayJdbcMigrationEngine flywayJdbcMigrationEngine;
    private volatile LiquibaseJdbcMigrationEngine liquibaseJdbcMigrationEngine;

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Class<PostgreSQLContainer<?>> getContainerType() {
        return (Class<PostgreSQLContainer<?>>) ((Class<?>) PostgreSQLContainer.class);
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerJdbc.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ContainerJdbcConnection.class;
    }

    @Override
    protected PostgreSQLContainer<?> getContainerDefault(PostgresJdbcMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse(PostgreSQLContainer.IMAGE));

        return new PostgreSQLContainer<>(dockerImage)
                .withDatabaseName("postgres")
                .withUsername("postgres")
                .withPassword("postgres")
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(PostgreSQLContainer.class))
                        .withMdc("image", metadata.image())
                        .withMdc("alias", metadata.networkAliasOrDefault()))
                .withNetworkAliases(metadata.networkAliasOrDefault());
    }

    @Override
    protected JdbcMigrationEngine getMigrationEngine(Migration.Engines engine, ExtensionContext context) {
        if (engine == Migration.Engines.FLYWAY) {
            if (this.flywayJdbcMigrationEngine == null) {
                var connection = getConnectionCurrent(context);
                this.flywayJdbcMigrationEngine = new FlywayJdbcMigrationEngine(connection);
            }
            return this.flywayJdbcMigrationEngine;
        } else if (engine == Migration.Engines.LIQUIBASE) {
            if (this.liquibaseJdbcMigrationEngine == null) {
                var connection = getConnectionCurrent(context);
                this.liquibaseJdbcMigrationEngine = new LiquibaseJdbcMigrationEngine(connection);
            }
            return this.liquibaseJdbcMigrationEngine;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        super.afterAll(context);
        if (this.flywayJdbcMigrationEngine != null) {
            this.flywayJdbcMigrationEngine.close();
            this.flywayJdbcMigrationEngine = null;
        }
        if (this.liquibaseJdbcMigrationEngine != null) {
            this.liquibaseJdbcMigrationEngine.close();
            this.liquibaseJdbcMigrationEngine = null;
        }
    }

    @NotNull
    protected Optional<PostgresJdbcMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersJdbc.class, context)
                .map(a -> new PostgresJdbcMetadata(false, null, a.image(), a.mode(), a.migration()));
    }

    @NotNull
    protected JdbcConnection getConnectionForContainer(PostgresJdbcMetadata metadata, @NotNull PostgreSQLContainer<?> container) {
        return JdbcConnectionImpl.forJDBC(container.getJdbcUrl(),
                container.getHost(),
                container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                container.getNetworkAliases().get(container.getNetworkAliases().size() - 1),
                PostgreSQLContainer.POSTGRESQL_PORT,
                container.getDatabaseName(),
                container.getUsername(),
                container.getPassword());
    }
}
