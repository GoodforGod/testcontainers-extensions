package io.goodforgod.testcontainers.extensions.mysql;

import io.goodforgod.testcontainers.extensions.jdbc.Migration;
import io.goodforgod.testcontainers.extensions.jdbc.MySQLContainerExtra;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

class MysqlExtraMigrationTests {

    @Test
    void flyway() {
        try (var container = new MySQLContainerExtra<>(DockerImageName.parse("mysql:8.0-debian"))) {
            container.start();
            container.migrate(Migration.Engines.FLYWAY, List.of("classpath:db/migration"));
            container.connection().assertQueriesNone("SELECT * FROM users;");
            container.drop(Migration.Engines.FLYWAY, List.of("classpath:db/migration"));
        }
    }

    @Test
    void liquibase() {
        try (var container = new MySQLContainerExtra<>(DockerImageName.parse("mysql:8.0-debian"))) {
            container.start();
            container.migrate(Migration.Engines.LIQUIBASE, List.of("db/changelog.sql"));
            container.connection().assertQueriesNone("SELECT * FROM users;");
            container.drop(Migration.Engines.LIQUIBASE, List.of("db/changelog.sql"));
        }
    }
}
