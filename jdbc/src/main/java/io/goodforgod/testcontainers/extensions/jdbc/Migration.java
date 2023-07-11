package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Migration {

    Engines engine();

    Mode apply();

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
        NONE,
        PER_CLASS,
        PER_METHOD
    }
}
