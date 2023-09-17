package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

final class TestcontainersCockroachdbExtension extends
        AbstractTestcontainersJdbcExtension<CockroachContainerExtra, CockroachMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersCockroachdbExtension.class);

    @Override
    protected Class<CockroachContainerExtra> getContainerType() {
        return CockroachContainerExtra.class;
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerCockroachdb.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ContainerCockroachdbConnection.class;
    }

    @Override
    protected CockroachContainerExtra getContainerDefault(CockroachMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("cockroachdb/cockroach"));

        var container = new CockroachContainerExtra(dockerImage);
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
    protected Optional<CockroachMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersCockroachdb.class, context)
                .map(a -> new CockroachMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(), a.migration()));
    }

    @NotNull
    protected JdbcConnection getConnectionForContainer(CockroachMetadata metadata, @NotNull CockroachContainerExtra container) {
        return container.connection();
    }
}
