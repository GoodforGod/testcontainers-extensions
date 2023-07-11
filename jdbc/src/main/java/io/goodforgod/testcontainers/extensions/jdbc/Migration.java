package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
     * @return will be by default "classpath:db/migration" for FlyWay and "db/migration/changelog.sql"
     *             for Liquibase
     */
    String[] migrations() default {};

    enum Engines {
        FLYWAY,
        LIQUIBASE
    }

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
