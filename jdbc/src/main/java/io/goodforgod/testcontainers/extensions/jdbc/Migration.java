package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes database container migrations between test executions
 */
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
     * @return will be by default "classpath:db/migration" for Flyway and "db/migration/changelog.sql"
     *             for Liquibase
     */
    String[] migrations() default {};

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
}
