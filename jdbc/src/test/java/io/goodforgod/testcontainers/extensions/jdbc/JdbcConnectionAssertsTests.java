package io.goodforgod.testcontainers.extensions.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

@TestcontainersJdbc(mode = ContainerMode.PER_CLASS,
        image = "postgres:15.2-alpine",
        migration = @Migration(
                engine = Migration.Engines.FLYWAY,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD))
class JdbcConnectionAssertsTests {

    @Test
    void execute(@ContainerJdbcConnection JdbcConnection connection) {
        assertThrows(JdbcConnectionException.class,
                () -> connection.execute("CREATE TABLE users (id INT NOT NULL PRIMARY KEY);"));
    }

    @Test
    void executeFromResources(@ContainerJdbcConnection JdbcConnection connection) {
        connection.executeFromResources("db/migration/V1__flyway.sql");
    }

    @Test
    void queryOne(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        var usersFound = connection.queryOne("SELECT * FROM users;", r -> r.getInt(1)).orElse(null);
        assertEquals(1, usersFound);
    }

    @Test
    void queryMany(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        connection.execute("INSERT INTO users VALUES(2);");
        var usersFound = connection.queryMany("SELECT * FROM users;", r -> r.getInt(1));
        assertEquals(2, usersFound.size());
    }

    @Test
    void count(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        assertEquals(1, connection.count("users"));
    }

    @Test
    void assertCountsNoneWhenMore(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsNone("users"));
    }

    @Test
    void assertCountsNoneWhenZero(@ContainerJdbcConnection JdbcConnection connection) {
        assertDoesNotThrow(() -> connection.assertCountsNone("users"));
    }

    @Test
    void assertCountsAtLeastWhenZero(@ContainerJdbcConnection JdbcConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsAtLeast(1, "users"));
    }

    @Test
    void assertCountsAtLeastWhenMore(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        connection.execute("INSERT INTO users VALUES(2);");
        assertDoesNotThrow(() -> connection.assertCountsAtLeast(1, "users"));
    }

    @Test
    void assertCountsAtLeastWhenEquals(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        assertDoesNotThrow(() -> connection.assertCountsAtLeast(1, "users"));
    }

    @Test
    void assertCountsExactWhenZero(@ContainerJdbcConnection JdbcConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsEquals(1, "users"));
    }

    @Test
    void assertCountsExactWhenMore(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        connection.execute("INSERT INTO users VALUES(2);");
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsEquals(1, "users"));
    }

    @Test
    void assertCountsExactWhenEquals(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        assertDoesNotThrow(() -> connection.assertCountsEquals(1, "users"));
    }

    @Test
    void assertQueriesNoneWhenMore(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        assertThrows(AssertionFailedError.class, () -> connection.assertQueriesNone("SELECT * FROM users;"));
    }

    @Test
    void assertQueriesNoneWhenZero(@ContainerJdbcConnection JdbcConnection connection) {
        assertDoesNotThrow(() -> connection.assertQueriesNone("SELECT * FROM users;"));
    }

    @Test
    void assertQueriesAtLeastWhenZero(@ContainerJdbcConnection JdbcConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertQueriesAtLeast(1, "SELECT * FROM users;"));
    }

    @Test
    void assertQueriesAtLeastWhenMore(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        connection.execute("INSERT INTO users VALUES(2);");
        assertDoesNotThrow(() -> connection.assertQueriesAtLeast(1, "SELECT * FROM users;"));
    }

    @Test
    void assertQueriesAtLeastWhenEquals(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        assertDoesNotThrow(() -> connection.assertQueriesAtLeast(1, "SELECT * FROM users;"));
    }

    @Test
    void assertQueriesExactWhenZero(@ContainerJdbcConnection JdbcConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertQueriesEquals(1, "SELECT * FROM users;"));
    }

    @Test
    void assertQueriesExactWhenMore(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        connection.execute("INSERT INTO users VALUES(2);");
        assertThrows(AssertionFailedError.class, () -> connection.assertQueriesEquals(1, "SELECT * FROM users;"));
    }

    @Test
    void assertQueriesExactWhenEquals(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        assertDoesNotThrow(() -> connection.assertQueriesEquals(1, "SELECT * FROM users;"));
    }

    @Test
    void assertInsertedNotThrows(@ContainerJdbcConnection JdbcConnection connection) {
        assertDoesNotThrow(() -> connection.assertInserted("INSERT INTO users VALUES(1);"));
    }

    @Test
    void assertInsertedThrows(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        assertThrows(AssertionFailedError.class,
                () -> connection.assertInserted("INSERT INTO users VALUES(1) ON CONFLICT (id) DO NOTHING;"));
    }

    @Test
    void assertUpdatedNotThrows(@ContainerJdbcConnection JdbcConnection connection) {
        assertDoesNotThrow(() -> connection.assertUpdated("INSERT INTO users VALUES(1);"));
    }

    @Test
    void assertUpdatedThrows(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        assertThrows(AssertionFailedError.class,
                () -> connection.assertUpdated("INSERT INTO users VALUES(1) ON CONFLICT (id) DO NOTHING;"));
    }

    @Test
    void assertDeletedNotThrows(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        assertDoesNotThrow(() -> connection.assertDeleted("DELETE FROM users;"));
    }

    @Test
    void assertDeletedThrows(@ContainerJdbcConnection JdbcConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertDeleted("DELETE FROM users;"));
    }

    @Test
    void checkQueriesNoneWhenMore(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        assertFalse(connection.checkQueriesNone("SELECT * FROM users;"));
    }

    @Test
    void checkQueriesNoneWhenZero(@ContainerJdbcConnection JdbcConnection connection) {
        assertTrue(connection.checkQueriesNone("SELECT * FROM users;"));
    }

    @Test
    void checkQueriesAtLeastWhenZero(@ContainerJdbcConnection JdbcConnection connection) {
        assertFalse(connection.checkQueriesAtLeast(1, "SELECT * FROM users;"));
    }

    @Test
    void checkQueriesAtLeastWhenMore(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        connection.execute("INSERT INTO users VALUES(2);");
        assertTrue(connection.checkQueriesAtLeast(1, "SELECT * FROM users;"));
    }

    @Test
    void checkQueriesAtLeastWhenEquals(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        assertTrue(connection.checkQueriesAtLeast(1, "SELECT * FROM users;"));
    }

    @Test
    void checkQueriesExactWhenZero(@ContainerJdbcConnection JdbcConnection connection) {
        assertFalse(connection.checkQueriesEquals(1, "SELECT * FROM users;"));
    }

    @Test
    void checkQueriesExactWhenMore(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        connection.execute("INSERT INTO users VALUES(2);");
        assertFalse(connection.checkQueriesEquals(1, "SELECT * FROM users;"));
    }

    @Test
    void checkQueriesExactWhenEquals(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        assertTrue(connection.checkQueriesEquals(1, "SELECT * FROM users;"));
    }

    @Test
    void checkInsertedNotThrows(@ContainerJdbcConnection JdbcConnection connection) {
        assertTrue(connection.checkInserted("INSERT INTO users VALUES(1);"));
    }

    @Test
    void checkInsertedThrows(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        assertFalse(connection.checkInserted("INSERT INTO users VALUES(1) ON CONFLICT (id) DO NOTHING;"));
    }

    @Test
    void checkUpdatedNotThrows(@ContainerJdbcConnection JdbcConnection connection) {
        assertTrue(connection.checkUpdated("INSERT INTO users VALUES(1);"));
    }

    @Test
    void checkUpdatedThrows(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        assertFalse(connection.checkUpdated("INSERT INTO users VALUES(1) ON CONFLICT (id) DO NOTHING;"));
    }

    @Test
    void checkDeletedNotThrows(@ContainerJdbcConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        assertTrue(connection.checkDeleted("DELETE FROM users;"));
    }

    @Test
    void checkDeletedThrows(@ContainerJdbcConnection JdbcConnection connection) {
        assertFalse(connection.checkDeleted("DELETE FROM users;"));
    }
}
