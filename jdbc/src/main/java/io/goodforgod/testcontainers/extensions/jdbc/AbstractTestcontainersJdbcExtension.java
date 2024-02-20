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

    private void tryMigrateIfRequired(JdbcMetadata metadata, ExtensionContext context) {
        JdbcMigrationEngine migrationEngine = getMigrationEngine(metadata.migration().engine(), context);
        migrationEngine.migrate(Arrays.asList(metadata.migration().migrations()));
    }

    private void tryDropIfRequired(JdbcMetadata metadata, ExtensionContext context) {
        JdbcMigrationEngine migrationEngine = getMigrationEngine(metadata.migration().engine(), context);
        migrationEngine.drop(Arrays.asList(metadata.migration().migrations()));
    }

    protected abstract JdbcMigrationEngine getMigrationEngine(Migration.Engines engine, ExtensionContext context);

    @Override
    public void beforeAll(ExtensionContext context) {
        super.beforeAll(context);

        var metadata = getMetadata(context);
        if (metadata.migration().apply() == Migration.Mode.PER_CLASS) {
            tryMigrateIfRequired(metadata, context);
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

        if (metadata.migration().apply() == Migration.Mode.PER_METHOD) {
            tryMigrateIfRequired(metadata, context);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var metadata = getMetadata(context);
        if (metadata.migration().drop() == Migration.Mode.PER_METHOD) {
            if (metadata.runMode() != ContainerMode.PER_METHOD) {
                tryDropIfRequired(metadata, context);
            }
        }

        super.afterEach(context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        var metadata = getMetadata(context);
        if (metadata.migration().drop() == Migration.Mode.PER_CLASS) {
            if (metadata.runMode() == ContainerMode.PER_RUN) {
                tryDropIfRequired(metadata, context);
            }
        }

        super.afterAll(context);
    }
}
