package io.goodforgod.testcontainers.extensions.redpanda;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Topics to set up right after container started
 */
@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Topics {

    /**
     * @return topics to set up right after container started (depends on {@link ContainerMode})
     */
    String[] value() default {};

    /**
     * @return mode when to reset (recreate) topics specified in {@link #value()}
     */
    Mode reset() default Mode.NONE;

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
