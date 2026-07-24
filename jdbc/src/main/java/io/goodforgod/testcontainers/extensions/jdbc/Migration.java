package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// @formatter:off
/**
 * Describes database container migrations between test executions.
 * <p>
 * Migration {@link Strategy#DEFAULT} runs the selected engine directly against the active
 * {@link JdbcConnection}. This is the historical behavior and remains the default.
 * <p>
 * Migration {@link Strategy#TEMPLATE_CLONE} is intended for {@link
 * io.goodforgod.testcontainers.extensions.Isolation.Mode#PER_METHOD}. A provider creates a migrated
 * template database once and then creates every isolated test database from that template. This can
 * be much faster than applying Flyway or Liquibase migrations for every test method. PostgreSQL
 * supports this strategy via {@code CREATE DATABASE ... TEMPLATE ...}; unsupported providers fail
 * fast when the strategy is selected.
 * <p>
 * Example:
 *
 * <pre>{@code
 * @TestcontainersPostgreSQL(
 *         mode = ContainerMode.PER_RUN,
 *         isolation = @Isolation(Isolation.Mode.PER_METHOD),
 *         migration = @Migration(
 *                 engine = Migration.Engines.FLYWAY,
 *                 apply = Migration.Mode.PER_CLASS,
 *                 drop = Migration.Mode.NONE,
 *                 strategy = Migration.Strategy.TEMPLATE_CLONE))
 * class RepositoryTests {}
 * }
 * </pre>
 */
// @formatter:on
@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Migration {

    /**
     * @return migration engine to use
     */
    Engines engine();

    /**
     * @return when to apply migrations
     */
    Mode apply();

    /**
     * @return when to drop migrations
     */
    Mode drop();

    /**
     * @return will be by default "classpath:db/migration" for Flyway and "db/changelog.sql"
     *             for Liquibase
     */
    String[] locations() default {};

    /**
     * @return migration setup strategy
     */
    Strategy strategy() default Strategy.DEFAULT;

    /**
     * Database migration engine implementation
     */
    enum Engines {
        /**
         * <a href=
         * "https://documentation.red-gate.com/fd/quickstart-how-flyway-works-184127223.html">Flyway</a>
         */
        FLYWAY,
        /**
         * <a href="https://docs.liquibase.com/concepts/introduction-to-liquibase.html">Liquibase</a>
         */
        LIQUIBASE
    }

    /**
     * apply / drop mode execution
     */
    enum Mode {
        /**
         * Indicates that will not run if specified
         */
        NONE,
        /**
         * Indicates that will run once per test class
         */
        PER_CLASS,
        /**
         * Indicates that will run each test method
         */
        PER_METHOD
    }

    /**
     * Migration setup strategy.
     */
    enum Strategy {
        /**
         * Runs migrations directly against the active connection.
         */
        DEFAULT,
        /**
         * Creates a migrated template database once and clones it for each isolated connection.
         * Requires {@code Isolation.Mode.PER_METHOD}. Currently PostgreSQL supports this strategy.
         */
        TEMPLATE_CLONE
    }
}
