package io.goodforgod.testcontainers.extensions.minio;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Helps with creation and cleanup for Minio buckets between tests executions
 */
@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Bucket {

    /**
     * @return bucket names
     */
    String[] value();

    /**
     * @return when to create bucket if not exist
     */
    Mode create();

    /**
     * @return when to drop bucket
     */
    Mode drop();

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
