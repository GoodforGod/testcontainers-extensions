package io.goodforgod.testcontainers.extensions.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

@TestcontainersJdbc(mode = ContainerMode.PER_CLASS, image = "postgres:15.2-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContainerFromAnnotationTests {

    private static final String CUSTOM = "user";

    @ContainerJdbc
    private static final PostgreSQLContainer<?> container = new PostgreSQLContainer<>()
            .withDatabaseName(CUSTOM)
            .withUsername(CUSTOM)
            .withPassword(CUSTOM)
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(PostgreSQLContainer.class)))
            .withNetwork(Network.SHARED);

    @Test
    void checkParams(@ContainerJdbcConnection JdbcConnection connection) {
        assertEquals(CUSTOM, connection.params().database());
        assertEquals(CUSTOM, connection.params().username());
        assertEquals(CUSTOM, connection.params().password());
    }
}
