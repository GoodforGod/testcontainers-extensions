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
                locations = { "migration/setup.cql" }))
class CassandraConnectionAssertsTests {

    @Test
    void execute(@ConnectionCassandra CassandraConnection connection) {
        assertThrows(CassandraConnectionException.class,
                () -> connection.execute("CREATE TABLE cassandra.users(id INT, PRIMARY KEY (id))"));
    }

    @Test
    void executeFromResources(@ConnectionCassandra CassandraConnection connection) {
        connection.executeFromResources("migration/setup.cql");
    }

    @Test
    void queryOne(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        var usersFound = connection.queryOne("SELECT * FROM cassandra.users;", r -> r.getInt(0)).orElse(null);
        assertEquals(1, usersFound);
    }

    @Test
    void queryMany(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO users(id) VALUES(2);");
        var usersFound = connection.queryMany("SELECT * FROM cassandra.users;", r -> r.getInt(0));
        assertEquals(2, usersFound.size());
    }

    @Test
    void count(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertEquals(1, connection.count("cassandra.users"));
    }

    @Test
    void assertCountsNoneWhenMore(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsNone("users"));
    }

    @Test
    void assertCountsNoneWhenZero(@ConnectionCassandra CassandraConnection connection) {
        assertDoesNotThrow(() -> connection.assertCountsNone("users"));
    }

    @Test
    void assertCountsAtLeastWhenZero(@ConnectionCassandra CassandraConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsAtLeast(1, "users"));
    }

    @Test
    void assertCountsAtLeastWhenMore(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO users(id) VALUES(2);");
        assertDoesNotThrow(() -> connection.assertCountsAtLeast(1, "users"));
    }

    @Test
    void assertCountsAtLeastWhenEquals(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertDoesNotThrow(() -> connection.assertCountsAtLeast(1, "cassandra.users"));
    }

    @Test
    void assertCountsExactWhenZero(@ConnectionCassandra CassandraConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsEquals(1, "cassandra.users"));
    }

    @Test
    void assertCountsExactWhenMore(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO users(id) VALUES(2);");
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsEquals(1, "cassandra.users"));
    }

    @Test
    void assertCountsExactWhenEquals(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertDoesNotThrow(() -> connection.assertCountsEquals(1, "cassandra.users"));
    }

    @Test
    void assertQueriesNoneWhenMore(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO users(id) VALUES(1);");
        assertThrows(AssertionFailedError.class, () -> connection.assertQueriesNone("SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesNoneWhenZero(@ConnectionCassandra CassandraConnection connection) {
        assertDoesNotThrow(() -> connection.assertQueriesNone("SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesAtLeastWhenZero(@ConnectionCassandra CassandraConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertQueriesAtLeast(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesAtLeastWhenMore(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO cassandra.users(id) VALUES(2);");
        assertDoesNotThrow(() -> connection.assertQueriesAtLeast(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesAtLeastWhenEquals(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertDoesNotThrow(() -> connection.assertQueriesAtLeast(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesExactWhenZero(@ConnectionCassandra CassandraConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertQueriesEquals(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesExactWhenMore(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO cassandra.users(id) VALUES(2);");
        assertThrows(AssertionFailedError.class, () -> connection.assertQueriesEquals(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void assertQueriesExactWhenEquals(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertDoesNotThrow(() -> connection.assertQueriesEquals(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesNoneWhenMore(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertFalse(connection.checkQueriesNone("SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesNoneWhenZero(@ConnectionCassandra CassandraConnection connection) {
        assertTrue(connection.checkQueriesNone("SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesAtLeastWhenZero(@ConnectionCassandra CassandraConnection connection) {
        assertFalse(connection.checkQueriesAtLeast(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesAtLeastWhenMore(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO cassandra.users(id) VALUES(2);");
        assertTrue(connection.checkQueriesAtLeast(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesAtLeastWhenEquals(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertTrue(connection.checkQueriesAtLeast(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesExactWhenZero(@ConnectionCassandra CassandraConnection connection) {
        assertFalse(connection.checkQueriesEquals(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesExactWhenMore(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO cassandra.users(id) VALUES(2);");
        assertFalse(connection.checkQueriesEquals(1, "SELECT * FROM cassandra.users;"));
    }

    @Test
    void checkQueriesExactWhenEquals(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        assertTrue(connection.checkQueriesEquals(1, "SELECT * FROM cassandra.users;"));
    }
}
