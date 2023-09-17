package io.goodforgod.testcontainers.extensions.mariadb;

import io.goodforgod.testcontainers.extensions.jdbc.MariaDBContainerExtra;
import io.goodforgod.testcontainers.extensions.jdbc.Migration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

class MariaExtraMigrationTests {

    @Test
    void flyway() {
        try (var container = new MariaDBContainerExtra<>(DockerImageName.parse("mariadb:11.0-jammy"))) {
            container.start();
            container.migrate(Migration.Engines.FLYWAY, List.of("classpath:db/migration"));
            container.connection().assertQueriesNone("SELECT * FROM users;");
            container.drop(Migration.Engines.FLYWAY, List.of("classpath:db/migration"));
        }
    }

    @Test
    void liquibase() {
        try (var container = new MariaDBContainerExtra<>(DockerImageName.parse("mariadb:11.0-jammy"))) {
            container.start();
            container.migrate(Migration.Engines.LIQUIBASE, List.of("db/changelog.sql"));
            container.connection().assertQueriesNone("SELECT * FROM users;");
            container.drop(Migration.Engines.LIQUIBASE, List.of("db/changelog.sql"));
        }
    }
}
