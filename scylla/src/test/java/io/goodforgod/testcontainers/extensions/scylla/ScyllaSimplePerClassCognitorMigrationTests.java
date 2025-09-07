package io.goodforgod.testcontainers.extensions.scylla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.*;

@TestcontainersScylla(mode = ContainerMode.PER_CLASS,
        migration = @Migration(
                engine = Migration.Engines.COGNITOR,
                apply = Migration.Mode.PER_CLASS,
                drop = Migration.Mode.PER_CLASS,
                locations = { "cognitor" }))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ScyllaSimplePerClassCognitorMigrationTests {

    @BeforeAll
    public static void setupAll(@ConnectionScylla ScyllaConnection paramConnection) {
        paramConnection.queryOne("SELECT * FROM cassandra.users;", r -> r.getInt(0));
        assertNotNull(paramConnection);
    }

    @BeforeEach
    public void setupEach(@ConnectionScylla ScyllaConnection paramConnection) {
        paramConnection.queryOne("SELECT * FROM users;", r -> r.getInt(0));
        assertNotNull(paramConnection);
    }

    @Order(1)
    @Test
    void firstRun(@ConnectionScylla ScyllaConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
    }

    @Order(2)
    @Test
    void secondRun(@ConnectionScylla ScyllaConnection connection) {
        var usersFound = connection.queryOne("SELECT * FROM users;", r -> r.getInt(0)).orElse(null);
        assertEquals(1, usersFound);
    }
}
