package io.goodforgod.testcontainers.extensions.scylla;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

@TestcontainersScylla(mode = ContainerMode.PER_CLASS,
        migration = @Migration(
                engine = Migration.Engines.SCRIPTS,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD,
                locations = { "migration/setup.cql" }))
class ScyllaConnectionAssertsTests {

    @Test
    void execute(@ConnectionScylla ScyllaConnection connection) {
        assertThrows(ScyllaConnectionException.class,
                () -> connection.execute("CREATE TABLE cassandra.users(id INT, PRIMARY KEY (id))"));
    }

    @Test
    void executeFromResources(@ConnectionScylla ScyllaConnection connection) {
        connection.executeFromResources("migration/setup.cql");
    }

    @Test
    void queryOne(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        var usersFound = connection.queryOne("SELECT * FROM cassandra.users;", r -> r.getInt(0)).orElse(null);
        assertEquals(1, usersFound);
    }

    @Test
    void queryMany(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO users(id) VALUES(2);");
        var usersFound = connection.queryMany("SELECT * FROM cassandra.users;", r -> r.getInt(0));
        assertEquals(2, usersFound.size());
    }

    @Test
    void count(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertEquals(1, connection.count("cassandra.users"));
    }

    @Test
    void assertCountsNoneWhenMore(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsNone("users"));
    }

    @Test
    void assertCountsNoneWhenZero(@ConnectionScylla ScyllaConnection connection) {
        assertDoesNotThrow(() -> connection.assertCountsNone("users"));
    }

    @Test
    void assertCountsAtLeastWhenZero(@ConnectionScylla ScyllaConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsAtLeast(1, "users"));
    }

    @Test
    void assertCountsAtLeastWhenMore(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO users(id) VALUES(2);");
        assertDoesNotThrow(() -> connection.assertCountsAtLeast(1, "users"));
    }

    @Test
    void assertCountsAtLeastWhenEquals(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertDoesNotThrow(() -> connection.assertCountsAtLeast(1, "cassandra.users"));
    }

    @Test
    void assertCountsExactWhenZero(@ConnectionScylla ScyllaConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsEquals(1, "cassandra.users"));
    }

    @Test
    void assertCountsExactWhenMore(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO users(id) VALUES(2);");
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsEquals(1, "cassandra.users"));
    }

    @Test
    void assertCountsExactWhenEquals(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertDoesNotThrow(() -> connection.assertCountsEquals(1, "cassandra.users"));
    }

    @Test
    void assertQueriesNoneWhenMore(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO users(id) VALUES(1);");
        assertThrows(AssertionFailedError.class, () -> connection.assertQueriesNone("SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesNoneWhenZero(@ConnectionScylla ScyllaConnection connection) {
        assertDoesNotThrow(() -> connection.assertQueriesNone("SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesAtLeastWhenZero(@ConnectionScylla ScyllaConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertQueriesAtLeast(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesAtLeastWhenMore(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO cassandra.users(id) VALUES(2);");
        assertDoesNotThrow(() -> connection.assertQueriesAtLeast(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesAtLeastWhenEquals(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertDoesNotThrow(() -> connection.assertQueriesAtLeast(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesExactWhenZero(@ConnectionScylla ScyllaConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertQueriesEquals(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesExactWhenMore(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO cassandra.users(id) VALUES(2);");
        assertThrows(AssertionFailedError.class, () -> connection.assertQueriesEquals(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesExactWhenEquals(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertDoesNotThrow(() -> connection.assertQueriesEquals(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesNoneWhenMore(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertFalse(connection.checkQueriesNone("SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesNoneWhenZero(@ConnectionScylla ScyllaConnection connection) {
        assertTrue(connection.checkQueriesNone("SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesAtLeastWhenZero(@ConnectionScylla ScyllaConnection connection) {
        assertFalse(connection.checkQueriesAtLeast(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesAtLeastWhenMore(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO cassandra.users(id) VALUES(2);");
        assertTrue(connection.checkQueriesAtLeast(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesAtLeastWhenEquals(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertTrue(connection.checkQueriesAtLeast(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesExactWhenZero(@ConnectionScylla ScyllaConnection connection) {
        assertFalse(connection.checkQueriesEquals(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesExactWhenMore(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO cassandra.users(id) VALUES(2);");
        assertFalse(connection.checkQueriesEquals(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesExactWhenEquals(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertTrue(connection.checkQueriesEquals(1, "SELECT * FROM cassandra.users;"));
    }
}
