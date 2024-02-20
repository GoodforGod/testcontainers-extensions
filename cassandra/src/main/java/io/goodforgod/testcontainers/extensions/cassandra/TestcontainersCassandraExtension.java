package io.goodforgod.testcontainers.extensions.cassandra;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.lang.annotation.Annotation;
import java.util.*;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

@Internal
class TestcontainersCassandraExtension extends
        AbstractTestcontainersExtension<CassandraConnection, CassandraContainerExtra<?>, CassandraMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersCassandraExtension.class);

    @Override
    protected Class<CassandraConnection> getConnectionType() {
        return CassandraConnection.class;
    }

    @Override
    protected CassandraContainerExtra<?> getContainerDefault(CassandraMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("cassandra"));

        var container = new CassandraContainerExtra<>(dockerImage);
        container.setNetworkAliases(new ArrayList<>(List.of(metadata.networkAliasOrDefault())));
        if (metadata.networkShared()) {
            container.withNetwork(Network.SHARED);
        }

        return container;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @SuppressWarnings("unchecked")
    protected Class<CassandraContainerExtra<?>> getContainerType() {
        return (Class<CassandraContainerExtra<?>>) ((Class<?>) CassandraContainerExtra.class);
    }

    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerCassandra.class;
    }

    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ContainerCassandraConnection.class;
    }

    @NotNull
    protected Optional<CassandraMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersCassandra.class, context)
                .map(a -> new CassandraMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(), a.migration()));
    }

    @NotNull
    protected CassandraConnection getConnectionForContainer(CassandraMetadata metadata, CassandraContainerExtra<?> container) {
        return container.connection();
    }

    private void tryMigrateIfRequired(CassandraMetadata annotation, CassandraConnection connection) {
        if (annotation.migration().engine() == Migration.Engines.SCRIPTS) {
            new ScriptCassandraMigrationEngine(connection).migrate(Arrays.asList(annotation.migration().migrations()));
        }
    }

    private void tryDropIfRequired(CassandraMetadata annotation, CassandraConnection connection) {
        if (annotation.migration().engine() == Migration.Engines.SCRIPTS) {
            new ScriptCassandraMigrationEngine(connection).drop(Arrays.asList(annotation.migration().migrations()));
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        super.beforeAll(context);

        var metadata = getMetadata(context);
        if (metadata.migration().apply() == Migration.Mode.PER_CLASS) {
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

        if (metadata.migration().apply() == Migration.Mode.PER_METHOD) {
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
