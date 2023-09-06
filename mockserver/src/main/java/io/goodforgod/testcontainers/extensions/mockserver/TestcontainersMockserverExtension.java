package io.goodforgod.testcontainers.extensions.mockserver;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@Internal
class TestcontainersMockserverExtension extends
        AbstractTestcontainersExtension<MockserverConnection, MockServerContainer, MockserverMetadata> {

    private static final String EXTERNAL_TEST_MOCKSERVER_HOST = "EXTERNAL_TEST_MOCKSERVER_HOST";
    private static final String EXTERNAL_TEST_MOCKSERVER_PORT = "EXTERNAL_TEST_MOCKSERVER_PORT";

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersMockserverExtension.class);

    protected Class<MockServerContainer> getContainerType() {
        return MockServerContainer.class;
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
    protected MockServerContainer getContainerDefault(MockserverMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("mockserver/mockserver"));

        var container = new MockServerContainer(dockerImage)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(MockServerContainer.class))
                        .withMdc("image", metadata.image())
                        .withMdc("alias", metadata.networkAliasOrDefault()))
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(5));

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
    protected MockserverConnection getConnectionForContainer(MockserverMetadata metadata,
                                                             @NotNull MockServerContainer container) {
        final String alias = container.getNetworkAliases().stream()
                .filter(a -> a.equals(metadata.networkAliasOrDefault()))
                .findFirst()
                .or(() -> container.getNetworkAliases().stream().findFirst())
                .orElse(null);

        return MockserverConnectionImpl.forContainer(container.getHost(),
                container.getMappedPort(MockServerContainer.PORT),
                alias,
                MockServerContainer.PORT);
    }

    @NotNull
    protected Optional<MockserverConnection> getConnectionExternal() {
        var host = System.getenv(EXTERNAL_TEST_MOCKSERVER_HOST);
        var port = System.getenv(EXTERNAL_TEST_MOCKSERVER_PORT);

        if (host != null && port != null) {
            return Optional.of(MockserverConnectionImpl.forExternal(host, Integer.parseInt(port)));
        } else
            return Optional.empty();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        super.afterEach(context);

        var connection = getConnectionCurrent(context);
        connection.client().reset();
    }
}
