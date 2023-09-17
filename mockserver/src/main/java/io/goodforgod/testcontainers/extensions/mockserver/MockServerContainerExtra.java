package io.goodforgod.testcontainers.extensions.mockserver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class MockServerContainerExtra extends MockServerContainer {

    private static final String EXTERNAL_TEST_MOCKSERVER_HOST = "EXTERNAL_TEST_MOCKSERVER_HOST";
    private static final String EXTERNAL_TEST_MOCKSERVER_PORT = "EXTERNAL_TEST_MOCKSERVER_PORT";

    private volatile MockserverConnectionImpl connection;

    public MockServerContainerExtra(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public MockServerContainerExtra(DockerImageName dockerImageName) {
        super(dockerImageName);

        final String alias = "mockserver-" + System.currentTimeMillis();
        this.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(MockServerContainerExtra.class))
                .withMdc("image", dockerImageName.asCanonicalNameString())
                .withMdc("alias", alias));
        this.waitingFor(Wait.forListeningPort());
        this.withStartupTimeout(Duration.ofMinutes(5));

        this.setNetworkAliases(new ArrayList<>(List.of(alias)));
    }

    @NotNull
    public MockserverConnection connection() {
        if (connection == null) {
            final Optional<MockserverConnection> connectionExternal = getConnectionExternal();
            if (connectionExternal.isEmpty() && !isRunning()) {
                throw new IllegalStateException("MockserverConnection can't be create for container that is not running");
            }

            final MockserverConnection jdbcConnection = connectionExternal.orElseGet(() -> {
                final String alias = getNetworkAliases().get(getNetworkAliases().size() - 1);
                return MockserverConnectionImpl.forContainer(getHost(),
                        getMappedPort(MockServerContainer.PORT),
                        alias,
                        MockServerContainer.PORT);
            });

            this.connection = (MockserverConnectionImpl) jdbcConnection;
        }

        return connection;
    }

    @Override
    public void start() {
        final Optional<MockserverConnection> connectionExternal = getConnectionExternal();
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
    private static Optional<MockserverConnection> getConnectionExternal() {
        var host = System.getenv(EXTERNAL_TEST_MOCKSERVER_HOST);
        var port = System.getenv(EXTERNAL_TEST_MOCKSERVER_PORT);

        if (host != null && port != null) {
            return Optional.of(MockserverConnectionImpl.forExternal(host, Integer.parseInt(port)));
        } else {
            return Optional.empty();
        }
    }
}
