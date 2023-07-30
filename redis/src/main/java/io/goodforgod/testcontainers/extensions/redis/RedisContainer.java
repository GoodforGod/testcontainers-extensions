package io.goodforgod.testcontainers.extensions.redis;

import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public final class RedisContainer extends GenericContainer<RedisContainer> {

    public static final Integer PORT = 6379;

    private static final String IMAGE_NAME = "redis";
    private static final DockerImageName IMAGE = DockerImageName.parse(IMAGE_NAME);

    private static final String DEFAULT_USER = "default";
    private static final String DEFAULT_PASSWORD = "redis";
    private static final int DEFAULT_DATABASE = 0;

    public RedisContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public RedisContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(IMAGE);
        withExposedPorts(PORT);
        withCommand("redis-server", "--requirepass " + DEFAULT_PASSWORD);
        waitingFor(Wait.forListeningPort());
        withStartupTimeout(Duration.ofSeconds(30));
    }

    public String getUser() {
        return DEFAULT_USER;
    }

    public String getPassword() {
        return DEFAULT_PASSWORD;
    }

    public int getDatabase() {
        return DEFAULT_DATABASE;
    }

    public int getPort() {
        return getMappedPort(PORT);
    }
}
