package io.goodforgod.testcontainers.extensions.redis;

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
class TestcontainersRedisExtension extends
        AbstractTestcontainersExtension<RedisConnection, RedisContainerExtra<?>, RedisMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersRedisExtension.class);

    @SuppressWarnings("unchecked")
    protected Class<RedisContainerExtra<?>> getContainerType() {
        return (Class<RedisContainerExtra<?>>) ((Class<?>) RedisContainerExtra.class);
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
    protected RedisContainerExtra<?> getContainerDefault(RedisMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("redis"));

        var container = new RedisContainerExtra<>(dockerImage);
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
    protected Optional<RedisMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersRedis.class, context)
                .map(a -> new RedisMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode()));
    }

    @NotNull
    protected RedisConnection getConnectionForContainer(RedisMetadata metadata, RedisContainerExtra<?> container) {
        return container.connection();
    }
}
