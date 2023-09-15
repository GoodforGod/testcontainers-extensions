package io.goodforgod.testcontainers.extensions.oracle;

import io.goodforgod.testcontainers.extensions.jdbc.Migration;
import io.goodforgod.testcontainers.extensions.jdbc.OracleContainerExtra;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

class OracleExtraMigrationTests {

    @Test
    void flyway() {
        try (var container = new OracleContainerExtra(DockerImageName.parse("gvenzl/oracle-xe:18.4.0-faststart"))) {
            container.start();
            container.migrate(Migration.Engines.FLYWAY, List.of("classpath:db/migration"));
            container.connection().assertQueriesNone("SELECT * FROM users;");
            container.drop(Migration.Engines.FLYWAY, List.of("classpath:db/migration"));
        }
    }

    @Test
    void liquibase() {
        try (var container = new OracleContainerExtra(DockerImageName.parse("gvenzl/oracle-xe:18.4.0-faststart"))) {
            container.start();
            container.migrate(Migration.Engines.LIQUIBASE, List.of("db/changelog.sql"));
            container.connection().assertQueriesNone("SELECT * FROM users;");
            container.drop(Migration.Engines.LIQUIBASE, List.of("db/changelog.sql"));
        }
    }
}
