package io.goodforgod.testcontainers.extensions.example;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.Optional;

class TestcontainersRedisExtension extends AbstractTestcontainersExtension<RedisConnection, RedisContainer, RedisMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersRedisExtension.class);

    protected Class<RedisContainer> getContainerType() {
        return RedisContainer.class;
    }

    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerRedis.class;
    }

    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ContainerRedisConnection.class;
    }

    @Override
    protected Class<RedisConnection> getConnectionType() {
        return RedisConnection.class;
    }

    @Override
    protected RedisContainer getContainerDefault(RedisMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("redis"));

        var container = new RedisContainer(dockerImage)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(RedisContainer.class))
                        .withMdc("image", metadata.image())
                        .withMdc("alias", metadata.networkAliasOrDefault()))
                .withNetworkAliases(metadata.networkAliasOrDefault())
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(5));

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
    protected Optional<RedisMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersRedis.class, context)
                .map(a -> new RedisMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode()));
    }

    @NotNull
    protected RedisConnection getConnectionForContainer(@NotNull RedisMetadata metadata, @NotNull RedisContainer container) {
        final String alias = Optional.ofNullable(metadata.networkAliasOrDefault())
                .filter(a -> !a.isBlank())
                .or(() -> container.getNetworkAliases().stream()
                        .filter(a -> a.startsWith("redis"))
                        .findFirst()
                        .or(() -> (container.getNetworkAliases().isEmpty())
                                ? Optional.empty()
                                : Optional.of(container.getNetworkAliases().get(container.getNetworkAliases().size() - 1))))
                .orElse(null);

        return RedisConnectionImpl.forContainer(container.getHost(),
                container.getMappedPort(RedisContainer.PORT),
                alias,
                RedisContainer.PORT,
                container.getDatabase(),
                container.getUser(),
                container.getPassword());
    }

    @NotNull
    protected Optional<RedisConnection> getConnectionExternal() {
        return Optional.empty();
    }
}
