package io.goodforgod.testcontainers.extensions.cassandra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersCassandra(mode = ContainerMode.PER_CLASS,
        migration = @Migration(
                engine = Migration.Engines.SCRIPTS,
                apply = Migration.Mode.PER_CLASS,
                drop = Migration.Mode.PER_CLASS,
                locations = { "migration" }))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CassandraSimplePerClassScriptMigrationTests {

    @BeforeAll
    public static void setupAll(@ConnectionCassandra CassandraConnection paramConnection) {
        paramConnection.queryOne("SELECT * FROM cassandra.users;", r -> r.getInt(0));
        assertNotNull(paramConnection);
    }

    @BeforeEach
    public void setupEach(@ConnectionCassandra CassandraConnection paramConnection) {
        paramConnection.queryOne("SELECT * FROM users;", r -> r.getInt(0));
        assertNotNull(paramConnection);
    }

    @Order(1)
    @Test
    void firstRun(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
    }

    @Order(2)
    @Test
    void secondRun(@ConnectionCassandra CassandraConnection connection) {
        var usersFound = connection.queryOne("SELECT * FROM users;", r -> r.getInt(0)).orElse(null);
        assertEquals(1, usersFound);
    }
}
