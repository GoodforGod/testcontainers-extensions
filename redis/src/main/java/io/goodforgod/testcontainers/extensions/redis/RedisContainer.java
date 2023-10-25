package io.goodforgod.testcontainers.extensions.redis;

import java.time.Duration;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class RedisContainer<SELF extends RedisContainer<SELF>> extends GenericContainer<SELF> {

    public static final Integer PORT = 6379;

    private static final String IMAGE_NAME = "redis";
    private static final DockerImageName IMAGE = DockerImageName.parse(IMAGE_NAME);

    private static final String DEFAULT_USER = "default";
    private static final String DEFAULT_PASSWORD = "redis";
    private static final int DEFAULT_DATABASE = 0;

    private Duration waitAfterStart = Duration.ZERO;

    public RedisContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public RedisContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(IMAGE);
        this.withExposedPorts(PORT);
        this.withCommand("redis-server", "--requirepass " + DEFAULT_PASSWORD);
        this.waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1));
        this.withStartupTimeout(Duration.ofSeconds(30));
    }

    @Override
    public void start() {
        super.start();

        if (waitAfterStart != Duration.ZERO) {
            try {
                Thread.sleep(waitAfterStart.toMillis());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
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

    public SELF waitAfterStart(@NotNull Duration duration) {
        this.waitAfterStart = duration;
        return self();
    }
}
