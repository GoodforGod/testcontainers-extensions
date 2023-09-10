package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

final class TestcontainersMariadbExtension extends
        AbstractTestcontainersJdbcExtension<MariaDBContainerExtra<?>, MariadbMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersMariadbExtension.class);

    @SuppressWarnings("unchecked")
    @Override
    protected Class<MariaDBContainerExtra<?>> getContainerType() {
        return (Class<MariaDBContainerExtra<?>>) ((Class<?>) MariaDBContainerExtra.class);
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerMariadb.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ContainerMariadbConnection.class;
    }

    @Override
    protected MariaDBContainerExtra<?> getContainerDefault(MariadbMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse(MariaDBContainer.NAME));

        var container = new MariaDBContainerExtra<>(dockerImage);
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
    protected Optional<MariadbMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersMariadb.class, context)
                .map(a -> new MariadbMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(), a.migration()));
    }

    @NotNull
    protected JdbcConnection getConnectionForContainer(MariadbMetadata metadata, @NotNull MariaDBContainerExtra<?> container) {
        return container.connection();
    }
}
