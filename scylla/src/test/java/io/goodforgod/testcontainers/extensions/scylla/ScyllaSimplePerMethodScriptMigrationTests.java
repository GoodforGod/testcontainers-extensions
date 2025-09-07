package io.goodforgod.testcontainers.extensions.scylla;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersScylla(mode = ContainerMode.PER_CLASS,
        migration = @Migration(
                engine = Migration.Engines.SCRIPTS,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD,
                dropMode = Migration.DropMode.DROP,
                locations = { "migration" }))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ScyllaSimplePerMethodScriptMigrationTests {

    @BeforeEach
    public void setupEach(@ConnectionScylla ScyllaConnection paramConnection) {
        paramConnection.queryOne("SELECT * FROM cassandra.users;", r -> r.getInt(0));
        assertNotNull(paramConnection);
    }

    @Order(1)
    @Test
    void firstRun(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO users(id) VALUES(1);");
    }

    @Order(2)
    @Test
    void secondRun(@ConnectionScylla ScyllaConnection connection) {
        var usersFound = connection.queryOne("SELECT * FROM cassandra.users;", r -> r.getInt(1));
        assertTrue(usersFound.isEmpty());
    }
}
