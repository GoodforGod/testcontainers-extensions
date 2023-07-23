package io.goodforgod.testcontainers.extensions.redis;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public final class RedisContainer extends GenericContainer<RedisContainer> {

    public static final Integer PORT = 6379;

    private static final String DEFAULT_USER = "redis";
    private static final String DEFAULT_PASSWORD = "redis";

    private String user = DEFAULT_USER;
    private String password = DEFAULT_PASSWORD;

    public RedisContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    public RedisContainer(String dockerImageName) {
        super(dockerImageName);
    }

    @Override
    protected void configure() {
        withEnv("REDIS_ARGS",
                "--requirepass password --user username on >password ~* allcommands --user default off nopass nocommands");
        waitingFor(Wait.forListeningPort());
    }

    public String getUser() {
        return user;
    }

    public RedisContainer withUser(String user) {
        this.user = user;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public RedisContainer withPassword(String password) {
        this.password = password;
        return this;
    }

    public int getPort() {
        return getMappedPort(PORT);
    }
}
