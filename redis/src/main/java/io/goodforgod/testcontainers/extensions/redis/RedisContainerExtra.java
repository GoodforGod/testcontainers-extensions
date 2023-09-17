package io.goodforgod.testcontainers.extensions.redis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

public class RedisContainerExtra<SELF extends RedisContainerExtra<SELF>> extends RedisContainer<SELF> {

    private static final String EXTERNAL_TEST_REDIS_USERNAME = "EXTERNAL_TEST_REDIS_USERNAME";
    private static final String EXTERNAL_TEST_REDIS_PASSWORD = "EXTERNAL_TEST_REDIS_PASSWORD";
    private static final String EXTERNAL_TEST_REDIS_HOST = "EXTERNAL_TEST_REDIS_HOST";
    private static final String EXTERNAL_TEST_REDIS_PORT = "EXTERNAL_TEST_REDIS_PORT";
    private static final String EXTERNAL_TEST_REDIS_DATABASE = "EXTERNAL_TEST_REDIS_DATABASE";

    private volatile RedisConnectionImpl connection;

    public RedisContainerExtra(String dockerImageName) {
        super(dockerImageName);
    }

    public RedisContainerExtra(DockerImageName dockerImageName) {
        super(dockerImageName);

        final String alias = "redis-" + System.currentTimeMillis();
        this.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(RedisContainerExtra.class))
                .withMdc("image", dockerImageName.asCanonicalNameString())
                .withMdc("alias", alias))
                .withStartupTimeout(Duration.ofMinutes(5));

        this.setNetworkAliases(new ArrayList<>(List.of(alias)));
    }

    @NotNull
    public RedisConnection connection() {
        if (connection == null) {
            final Optional<RedisConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !isRunning()) {
                throw new IllegalStateException("RedisConnection can't be create for container that is not running");
            }

            final RedisConnection redisConnection = connectionExternal.orElseGet(() -> {
                final String alias = getNetworkAliases().get(getNetworkAliases().size() - 1);
                return RedisConnectionImpl.forContainer(getHost(),
                        getMappedPort(RedisContainer.PORT),
                        alias,
                        RedisContainer.PORT,
                        getDatabase(),
                        getUser(),
                        getPassword());
            });

            this.connection = (RedisConnectionImpl) redisConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<RedisConnection> connectionExternal = getConnectionExternal();
        if (connectionExternal.isEmpty()) {
            super.start();
        }
    }

    @Override
    public void stop() {
        connection.close();
        connection = null;
        super.stop();
    }

    @NotNull
    private static Optional<RedisConnection> getConnectionExternal() {
        var host = System.getenv(EXTERNAL_TEST_REDIS_HOST);
        var port = System.getenv(EXTERNAL_TEST_REDIS_PORT);
        var user = System.getenv(EXTERNAL_TEST_REDIS_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_REDIS_PASSWORD);
        var database = Optional.ofNullable(System.getenv(EXTERNAL_TEST_REDIS_DATABASE)).map(Integer::parseInt).orElse(0);

        if (host != null && port != null) {
            return Optional.of(RedisConnectionImpl.forExternal(host, Integer.parseInt(port), database, user, password));
        } else {
            return Optional.empty();
        }
    }
}
