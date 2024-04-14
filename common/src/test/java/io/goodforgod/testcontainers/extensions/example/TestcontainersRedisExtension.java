package io.goodforgod.testcontainers.extensions.example;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

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
        return ConnectionRedis.class;
    }

    @Override
    protected Class<RedisConnection> getConnectionType() {
        return RedisConnection.class;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @Override
    protected RedisContainer createContainerDefault(RedisMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("redis"));

        String alias = Optional.ofNullable(metadata.networkAlias()).orElseGet(() -> "redis-" + System.currentTimeMillis());
        var container = new RedisContainer(dockerImage)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(RedisContainer.class))
                        .withMdc("image", metadata.image()))
                .withNetworkAliases(alias)
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(5));

        if (metadata.networkShared()) {
            container.withNetwork(Network.SHARED);
        }

        return container;
    }

    @Override
    protected ContainerContext<RedisConnection> createContainerContext(RedisContainer container) {
        return new RedisContext(container);
    }

    @NotNull
    protected Optional<RedisMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersRedis.class, context)
                .map(a -> new RedisMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode()));
    }

    @NotNull
    protected Optional<RedisConnection> getConnectionExternal() {
        return Optional.empty();
    }
}
