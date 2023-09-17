package io.goodforgod.testcontainers.extensions.jdbc;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

class CockroachdbExtraMigrationTests {

    @Test
    void flyway() {
        try (var container = new CockroachContainerExtra(DockerImageName.parse("cockroachdb/cockroach:latest-v23.1"))) {
            container.start();
            container.migrate(Migration.Engines.FLYWAY, List.of("classpath:db/migration"));
            container.connection().assertQueriesNone("SELECT * FROM users;");
            container.drop(Migration.Engines.FLYWAY, List.of("classpath:db/migration"));
        }
    }

    @Test
    void liquibase() {
        try (var container = new CockroachContainerExtra(DockerImageName.parse("cockroachdb/cockroach:latest-v23.1"))) {
            container.start();
            container.migrate(Migration.Engines.LIQUIBASE, List.of("db/changelog.sql"));
            container.connection().assertQueriesNone("SELECT * FROM users;");
            container.drop(Migration.Engines.LIQUIBASE, List.of("db/changelog.sql"));
        }
    }
}
