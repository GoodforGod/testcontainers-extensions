package io.goodforgod.testcontainers.extensions.cassandra;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

class CassandraExtraMigrationTests {

    @Test
    void script() {
        try (var container = new CassandraContainerExtra<>(DockerImageName.parse("cassandra:4.1"))) {
            container.start();
            ScriptCassandraMigrationEngine scriptCassandraMigrationEngine = new ScriptCassandraMigrationEngine(
                    container.connection());
            scriptCassandraMigrationEngine.migrate(List.of("migration"));
            container.connection().assertQueriesNone("SELECT * FROM cassandra.users;");
            scriptCassandraMigrationEngine.drop(List.of("migration"));
        }
    }
}
