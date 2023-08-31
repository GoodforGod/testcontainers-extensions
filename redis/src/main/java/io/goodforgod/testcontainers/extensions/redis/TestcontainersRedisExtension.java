package io.goodforgod.testcontainers.extensions.redis;

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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@Internal
class TestcontainersRedisExtension extends AbstractTestcontainersExtension<RedisConnection, RedisContainer, RedisMetadata> {

    private static final String EXTERNAL_TEST_REDIS_USERNAME = "EXTERNAL_TEST_REDIS_USERNAME";
    private static final String EXTERNAL_TEST_REDIS_PASSWORD = "EXTERNAL_TEST_REDIS_PASSWORD";
    private static final String EXTERNAL_TEST_REDIS_HOST = "EXTERNAL_TEST_REDIS_HOST";
    private static final String EXTERNAL_TEST_REDIS_PORT = "EXTERNAL_TEST_REDIS_PORT";
    private static final String EXTERNAL_TEST_REDIS_DATABASE = "EXTERNAL_TEST_REDIS_DATABASE";

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
    protected Optional<RedisMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersRedis.class, context)
                .map(a -> new RedisMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode()));
    }

    @NotNull
    protected RedisConnection getConnectionForContainer(RedisMetadata metadata, @NotNull RedisContainer container) {
        final String alias = container.getNetworkAliases().stream()
                .filter(a -> a.equals(metadata.networkAliasOrDefault()))
                .findFirst()
                .or(() -> container.getNetworkAliases().stream().findFirst())
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
        var host = System.getenv(EXTERNAL_TEST_REDIS_HOST);
        var port = System.getenv(EXTERNAL_TEST_REDIS_PORT);
        var user = System.getenv(EXTERNAL_TEST_REDIS_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_REDIS_PASSWORD);
        var database = Optional.ofNullable(System.getenv(EXTERNAL_TEST_REDIS_DATABASE)).map(Integer::parseInt).orElse(0);

        if (host != null && port != null) {
            return Optional.of(RedisConnectionImpl.forExternal(host, Integer.parseInt(port), database, user, password));
        } else
            return Optional.empty();
    }
}
