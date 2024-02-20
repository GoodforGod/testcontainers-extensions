package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

final class TestcontainersPostgresExtension extends
        AbstractTestcontainersJdbcExtension<PostgreSQLContainerExtra<?>, PostgresMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersPostgresExtension.class);

    @SuppressWarnings("unchecked")
    @Override
    protected Class<PostgreSQLContainerExtra<?>> getContainerType() {
        return (Class<PostgreSQLContainerExtra<?>>) ((Class<?>) PostgreSQLContainerExtra.class);
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerPostgres.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ContainerPostgresConnection.class;
    }

    @Override
    protected PostgreSQLContainerExtra<?> getContainerDefault(PostgresMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse(PostgreSQLContainer.IMAGE));

        var container = new PostgreSQLContainerExtra<>(dockerImage);
        container.setNetworkAliases(new ArrayList<>(List.of(metadata.networkAliasOrDefault())));
        if (metadata.networkShared()) {
            container.withNetwork(Network.SHARED);
        }

        return container;
    }

    @Override
    protected JdbcMigrationEngine getMigrationEngine(Migration.Engines engine, ExtensionContext context) {
        var containerCurrent = getContainerCurrent(context);
        return containerCurrent.getMigrationEngine(engine);
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @NotNull
    protected Optional<PostgresMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersPostgres.class, context)
                .map(a -> new PostgresMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(), a.migration()));
    }

    @NotNull
    protected JdbcConnection getConnectionForContainer(PostgresMetadata metadata,
                                                       @NotNull PostgreSQLContainerExtra<?> container) {
        return container.connection();
    }
}
