package io.goodforgod.testcontainers.extensions.cassandra;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersCassandra(mode = ContainerMode.PER_CLASS,
        migration = @Migration(
                engine = Migration.Engines.COGNITOR,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD,
                dropMode = Migration.DropMode.DROP,
                locations = { "cognitor" }))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CassandraSimplePerMethodCognitorMigrationTests {

    @BeforeEach
    public void setupEach(@ConnectionCassandra CassandraConnection paramConnection) {
        paramConnection.queryOne("SELECT * FROM cassandra.users;", r -> r.getInt(0));
        assertNotNull(paramConnection);
    }

    @Order(1)
    @Test
    void firstRun(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO users(id) VALUES(1);");
    }

    @Order(2)
    @Test
    void secondRun(@ConnectionCassandra CassandraConnection connection) {
        var usersFound = connection.queryOne("SELECT * FROM cassandra.users;", r -> r.getInt(1));
        assertTrue(usersFound.isEmpty());
    }
}
