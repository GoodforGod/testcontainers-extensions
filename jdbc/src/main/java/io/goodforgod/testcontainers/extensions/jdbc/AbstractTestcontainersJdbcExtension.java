package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.util.Arrays;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.JdbcDatabaseContainer;

@Internal
abstract class AbstractTestcontainersJdbcExtension<Container extends JdbcDatabaseContainer<?>, Metadata extends JdbcMetadata>
        extends
        AbstractTestcontainersExtension<JdbcConnection, Container, Metadata> {

    @Override
    protected Class<JdbcConnection> getConnectionType() {
        return JdbcConnection.class;
    }

    private void tryMigrateIfRequired(JdbcMetadata metadata, JdbcConnection connection) {
        if (metadata.migration().engine() == Migration.Engines.FLYWAY) {
            FlywayJdbcMigrationEngine.INSTANCE.migrate(connection, Arrays.asList(metadata.migration().migrations()));
        } else if (metadata.migration().engine() == Migration.Engines.LIQUIBASE) {
            LiquibaseJdbcMigrationEngine.INSTANCE.migrate(connection, Arrays.asList(metadata.migration().migrations()));
        }
    }

    private void tryDropIfRequired(JdbcMetadata metadata, JdbcConnection connection) {
        if (metadata.migration().engine() == Migration.Engines.FLYWAY) {
            FlywayJdbcMigrationEngine.INSTANCE.drop(connection, Arrays.asList(metadata.migration().migrations()));
        } else if (metadata.migration().engine() == Migration.Engines.LIQUIBASE) {
            LiquibaseJdbcMigrationEngine.INSTANCE.drop(connection, Arrays.asList(metadata.migration().migrations()));
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        super.beforeAll(context);

        var metadata = getMetadata(context);
        if (metadata.migration().apply() != Migration.Mode.NONE) {
            var storage = getStorage(context);
            var connectionCurrent = getConnectionCurrent(context);
            tryMigrateIfRequired(metadata, connectionCurrent);
            storage.put(Migration.class, metadata.migration().apply());
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var metadata = getMetadata(context);
        if (metadata.runMode() == ContainerMode.PER_METHOD && metadata.migration().apply() == Migration.Mode.PER_CLASS) {
            throw new ExtensionConfigurationException(String.format(
                    "@%s can't apply migration in Migration.Mode.PER_CLASS mode when ContainerMode.PER_METHOD is used",
                    getContainerAnnotation().getSimpleName()));
        }

        super.beforeEach(context);

        var storage = getStorage(context);
        var mode = storage.get(Migration.class, Migration.Mode.class);
        if (mode == null) {
            var connectionCurrent = getConnectionCurrent(context);
            tryMigrateIfRequired(metadata, connectionCurrent);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var metadata = getMetadata(context);
        var storage = getStorage(context);
        storage.remove(Migration.class);
        if (metadata.migration().drop() == Migration.Mode.PER_METHOD) {
            if (metadata.runMode() != ContainerMode.PER_METHOD) {
                var connectionCurrent = getConnectionCurrent(context);
                tryDropIfRequired(metadata, connectionCurrent);
            }
        }

        super.afterEach(context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        var metadata = getMetadata(context);
        var connectionCurrent = getConnectionCurrent(context);
        if (metadata.migration().drop() == Migration.Mode.PER_CLASS) {
            if (metadata.runMode() == ContainerMode.PER_RUN) {
                tryDropIfRequired(metadata, connectionCurrent);
            }
        }

        super.afterAll(context);
    }
}
