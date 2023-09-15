package io.goodforgod.testcontainers.extensions.cassandra;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

class CassandraExtraMigrationTests {

    @Test
    void script() {
        try (var container = new CassandraContainerExtra<>(DockerImageName.parse("cassandra:4.1"))) {
            container.start();
            container.migrate(Migration.Engines.SCRIPTS, List.of("migration"));
            container.connection().assertQueriesNone("SELECT * FROM cassandra.users;");
            container.drop(Migration.Engines.SCRIPTS, List.of("migration"));
        }
    }
}
