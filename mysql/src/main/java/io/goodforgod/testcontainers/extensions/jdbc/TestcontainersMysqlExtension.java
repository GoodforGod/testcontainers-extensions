package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

final class TestcontainersMysqlExtension extends AbstractTestcontainersJdbcExtension<MySQLContainerExtra<?>, MysqlMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersMysqlExtension.class);

    @SuppressWarnings("unchecked")
    @Override
    protected Class<MySQLContainerExtra<?>> getContainerType() {
        return (Class<MySQLContainerExtra<?>>) ((Class<?>) MySQLContainerExtra.class);
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerMysql.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ContainerMysqlConnection.class;
    }

    @Override
    protected MySQLContainerExtra<?> getContainerDefault(MysqlMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse(MySQLContainer.NAME));

        var container = new MySQLContainerExtra<>(dockerImage);
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

    @NotNull
    protected Optional<MysqlMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersMysql.class, context)
                .map(a -> new MysqlMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(), a.migration()));
    }

    @NotNull
    protected JdbcConnection getConnectionForContainer(MysqlMetadata metadata, @NotNull MySQLContainerExtra<?> container) {
        return container.connection();
    }
}
