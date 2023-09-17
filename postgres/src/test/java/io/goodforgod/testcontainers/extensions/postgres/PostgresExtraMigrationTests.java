package io.goodforgod.testcontainers.extensions.postgres;

import io.goodforgod.testcontainers.extensions.jdbc.Migration;
import io.goodforgod.testcontainers.extensions.jdbc.PostgreSQLContainerExtra;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

class PostgresExtraMigrationTests {

    @Test
    void flyway() {
        try (var container = new PostgreSQLContainerExtra<>(DockerImageName.parse("postgres:15.3-alpine"))) {
            container.start();
            container.migrate(Migration.Engines.FLYWAY, List.of("classpath:db/migration"));
            container.connection().assertQueriesNone("SELECT * FROM users;");
            container.drop(Migration.Engines.FLYWAY, List.of("classpath:db/migration"));
        }
    }

    @Test
    void liquibase() {
        try (var container = new PostgreSQLContainerExtra<>(DockerImageName.parse("postgres:15.3-alpine"))) {
            container.start();
            container.migrate(Migration.Engines.LIQUIBASE, List.of("db/changelog.sql"));
            container.connection().assertQueriesNone("SELECT * FROM users;");
            container.drop(Migration.Engines.LIQUIBASE, List.of("db/changelog.sql"));
        }
    }
}
