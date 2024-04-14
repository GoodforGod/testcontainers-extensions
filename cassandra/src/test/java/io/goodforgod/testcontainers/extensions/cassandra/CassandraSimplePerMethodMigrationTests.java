package io.goodforgod.testcontainers.extensions.cassandra;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersCassandra(mode = ContainerMode.PER_CLASS,
        migration = @Migration(
                engine = Migration.Engines.SCRIPTS,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD,
                locations = { "migration" }))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CassandraSimplePerMethodMigrationTests {

    @BeforeEach
    public void setupEach(@ConnectionCassandra CassandraConnection paramConnection) {
        paramConnection.queryOne("SELECT * FROM cassandra.users;", r -> r.getInt(0));
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
        var usersFound = connection.queryOne("SELECT * FROM cassandra.users;", r -> r.getInt(1));
        assertTrue(usersFound.isEmpty());
    }
}
