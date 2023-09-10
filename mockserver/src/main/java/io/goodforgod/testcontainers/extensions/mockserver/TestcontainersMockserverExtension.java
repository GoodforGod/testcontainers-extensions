package io.goodforgod.testcontainers.extensions.mockserver;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

@Internal
class TestcontainersMockserverExtension extends
        AbstractTestcontainersExtension<MockserverConnection, MockServerContainerExtra, MockserverMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersMockserverExtension.class);

    protected Class<MockServerContainerExtra> getContainerType() {
        return MockServerContainerExtra.class;
    }

    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerMockserver.class;
    }

    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ContainerMockserverConnection.class;
    }

    @Override
    protected Class<MockserverConnection> getConnectionType() {
        return MockserverConnection.class;
    }

    @Override
    protected MockServerContainerExtra getContainerDefault(MockserverMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("mockserver/mockserver"));

        var container = new MockServerContainerExtra(dockerImage);

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
    protected Optional<MockserverMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersMockserver.class, context)
                .map(a -> new MockserverMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode()));
    }

    @NotNull
    protected MockserverConnection getConnectionForContainer(MockserverMetadata metadata, MockServerContainerExtra container) {
        return container.connection();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        super.beforeEach(context);

        var connection = getConnectionCurrent(context);
        connection.client().reset();
    }
}
