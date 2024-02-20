package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

final class TestcontainersOracleExtension extends AbstractTestcontainersJdbcExtension<OracleContainerExtra, OracleMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersOracleExtension.class);

    @Override
    protected Class<OracleContainerExtra> getContainerType() {
        return OracleContainerExtra.class;
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerOracle.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ContainerOracleConnection.class;
    }

    @Override
    protected OracleContainerExtra getContainerDefault(OracleMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("gvenzl/oracle-xe"));

        var container = new OracleContainerExtra(dockerImage);
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
    protected Optional<OracleMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersOracle.class, context)
                .map(a -> new OracleMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(), a.migration()));
    }

    @NotNull
    protected JdbcConnection getConnectionForContainer(OracleMetadata metadata, @NotNull OracleContainerExtra container) {
        return container.connection();
    }
}
