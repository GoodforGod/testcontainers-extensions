package io.goodforgod.testcontainers.extensions.redis;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@Internal
class TestcontainersRedisExtension extends
        AbstractTestcontainersExtension<RedisConnection, RedisContainer, RedisMetadata> {

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
        var image = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("redis"));

        final RedisContainer container = new RedisContainer(image)
                .waitAfterStart(Duration.ofMillis(25)); // cause some drivers tends to fail to connect to redis on hot start
        final String alias = Optional.ofNullable(metadata.networkAlias()).orElseGet(() -> "redis-" + System.currentTimeMillis());
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(RedisContainer.class), true)
                .withMdc("image", image.asCanonicalNameString())
                .withMdc("alias", alias))
                .withStartupTimeout(Duration.ofMinutes(2));
        container.setNetworkAliases(new ArrayList<>(List.of(alias)));
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
}
