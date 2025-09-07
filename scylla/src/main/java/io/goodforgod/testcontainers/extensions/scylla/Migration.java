package io.goodforgod.testcontainers.extensions.scylla;

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
     * @return what tactic to use: DROP TABLE or TRUNCATE TABLE (drop table is slow)
     */
    DropMode dropMode() default DropMode.TRUNCATE;

    /**
     * @return path for resource directory with scripts or scripts itself
     */
    String[] locations();

    /**
     * Database migration engine implementation
     */
    enum Engines {
        /**
         * For apply use scripts in ASC order to execute that are pretended to set up tables and data
         * For drop clean all Non System tables in all scylla
         */
        SCRIPTS,
        /**
         * Third-Party Scylla Migration library
         * <a href="https://github.com/patka/cassandra-migration">Github</a>
         */
        COGNITOR
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

    enum DropMode {
        TRUNCATE,
        DROP
    }
}
