package io.goodforgod.testcontainers.extensions.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestcontainersJdbc(mode = ContainerMode.PER_CLASS, image = "postgres:15.2-alpine")
abstract class ContainerPerClassAbstractTests {

    @ContainerJdbcConnection
    protected JdbcConnection sameConnectionParent;

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public static class TestClassChild extends ContainerPerClassAbstractTests {

        private final JdbcConnection sameConnectionChild;

        private static JdbcConnection firstConnection;

        TestClassChild(@ContainerJdbcConnection JdbcConnection sameConnectionChild) {
            this.sameConnectionChild = sameConnectionChild;
            assertNotNull(sameConnectionChild);
        }

        @Order(1)
        @Test
        void firstConnection(@ContainerJdbcConnection JdbcConnection connection) {
            assertNull(firstConnection);
            assertNotNull(connection);
            assertNotNull(connection.params().jdbcUrl());
            assertNotNull(sameConnectionChild);
            assertEquals(sameConnectionChild, connection);
            assertEquals(sameConnectionChild, sameConnectionParent);
            firstConnection = connection;
        }

        @Order(2)
        @Test
        void secondConnection(@ContainerJdbcConnection JdbcConnection connection) {
            assertNotNull(connection);
            assertNotNull(connection.params().jdbcUrl());
            assertNotNull(firstConnection);
            assertNotNull(sameConnectionChild);
            assertEquals(sameConnectionChild, connection);
            assertEquals(sameConnectionChild, sameConnectionParent);
            assertEquals(firstConnection, connection);
        }
    }
}
