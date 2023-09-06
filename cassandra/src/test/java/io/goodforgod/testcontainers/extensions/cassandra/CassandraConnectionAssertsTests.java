package io.goodforgod.testcontainers.extensions.cassandra;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

@TestcontainersCassandra(mode = ContainerMode.PER_CLASS,
        migration = @Migration(
                engine = Migration.Engines.SCRIPTS,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD,
                migrations = { "migration/setup.cql" }))
class CassandraConnectionAssertsTests {

    @Test
    void execute(@ContainerCassandraConnection CassandraConnection connection) {
        assertThrows(CassandraConnectionException.class,
                () -> connection.execute("CREATE TABLE cassandra.users(id INT, PRIMARY KEY (id))"));
    }

    @Test
    void executeFromResources(@ContainerCassandraConnection CassandraConnection connection) {
        connection.executeFromResources("migration/setup.cql");
    }

    @Test
    void queryOne(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        var usersFound = connection.queryOne("SELECT * FROM cassandra.users;", r -> r.getInt(0)).orElse(null);
        assertEquals(1, usersFound);
    }

    @Test
    void queryMany(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO cassandra.users(id) VALUES(2);");
        var usersFound = connection.queryMany("SELECT * FROM cassandra.users;", r -> r.getInt(0));
        assertEquals(2, usersFound.size());
    }

    @Test
    void count(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertEquals(1, connection.count("cassandra.users"));
    }

    @Test
    void assertCountsNoneWhenMore(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsNone("cassandra.users"));
    }

    @Test
    void assertCountsNoneWhenZero(@ContainerCassandraConnection CassandraConnection connection) {
        assertDoesNotThrow(() -> connection.assertCountsNone("cassandra.users"));
    }

    @Test
    void assertCountsAtLeastWhenZero(@ContainerCassandraConnection CassandraConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsAtLeast(1, "cassandra.users"));
    }

    @Test
    void assertCountsAtLeastWhenMore(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO cassandra.users(id) VALUES(2);");
        assertDoesNotThrow(() -> connection.assertCountsAtLeast(1, "cassandra.users"));
    }

    @Test
    void assertCountsAtLeastWhenEquals(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertDoesNotThrow(() -> connection.assertCountsAtLeast(1, "cassandra.users"));
    }

    @Test
    void assertCountsExactWhenZero(@ContainerCassandraConnection CassandraConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsEquals(1, "cassandra.users"));
    }

    @Test
    void assertCountsExactWhenMore(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO cassandra.users(id) VALUES(2);");
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsEquals(1, "cassandra.users"));
    }

    @Test
    void assertCountsExactWhenEquals(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertDoesNotThrow(() -> connection.assertCountsEquals(1, "cassandra.users"));
    }

    @Test
    void assertQueriesNoneWhenMore(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertThrows(AssertionFailedError.class, () -> connection.assertQueriesNone("SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesNoneWhenZero(@ContainerCassandraConnection CassandraConnection connection) {
        assertDoesNotThrow(() -> connection.assertQueriesNone("SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesAtLeastWhenZero(@ContainerCassandraConnection CassandraConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertQueriesAtLeast(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesAtLeastWhenMore(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO cassandra.users(id) VALUES(2);");
        assertDoesNotThrow(() -> connection.assertQueriesAtLeast(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesAtLeastWhenEquals(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertDoesNotThrow(() -> connection.assertQueriesAtLeast(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesExactWhenZero(@ContainerCassandraConnection CassandraConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertQueriesEquals(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesExactWhenMore(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO cassandra.users(id) VALUES(2);");
        assertThrows(AssertionFailedError.class, () -> connection.assertQueriesEquals(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesExactWhenEquals(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertDoesNotThrow(() -> connection.assertQueriesEquals(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesNoneWhenMore(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertFalse(connection.checkQueriesNone("SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesNoneWhenZero(@ContainerCassandraConnection CassandraConnection connection) {
        assertTrue(connection.checkQueriesNone("SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesAtLeastWhenZero(@ContainerCassandraConnection CassandraConnection connection) {
        assertFalse(connection.checkQueriesAtLeast(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesAtLeastWhenMore(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO cassandra.users(id) VALUES(2);");
        assertTrue(connection.checkQueriesAtLeast(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesAtLeastWhenEquals(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertTrue(connection.checkQueriesAtLeast(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesExactWhenZero(@ContainerCassandraConnection CassandraConnection connection) {
        assertFalse(connection.checkQueriesEquals(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesExactWhenMore(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO cassandra.users(id) VALUES(2);");
        assertFalse(connection.checkQueriesEquals(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesExactWhenEquals(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertTrue(connection.checkQueriesEquals(1, "SELECT * FROM cassandra.users;"));
    }
}
