package io.goodforgod.testcontainers.extensions.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.jdbc.example.ContainerJdbc;
import io.goodforgod.testcontainers.extensions.jdbc.example.ContainerJdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.example.TestcontainersJdbc;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

@TestcontainersJdbc(mode = ContainerMode.PER_METHOD, image = "postgres:15.2-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerFromAnnotationTests {

    private static final String CUSTOM = "user";

    @ContainerJdbc
    private static final PostgreSQLContainer<?> container = new PostgreSQLContainer<>()
            .withDatabaseName(CUSTOM)
            .withUsername(CUSTOM)
            .withPassword(CUSTOM)
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(PostgreSQLContainer.class)));

    @Test
    void checkParams(@ContainerJdbcConnection JdbcConnection connection) {
        assertEquals(CUSTOM, connection.params().database());
        assertEquals(CUSTOM, connection.params().username());
        assertEquals(CUSTOM, connection.params().password());
    }

    @Test
    void checkParamsAgain(@ContainerJdbcConnection JdbcConnection connection) {
        assertEquals(CUSTOM, connection.params().database());
        assertEquals(CUSTOM, connection.params().username());
        assertEquals(CUSTOM, connection.params().password());
    }
}
