package io.goodforgod.testcontainers.extensions.jdbc;

import org.testcontainers.containers.JdbcDatabaseContainer;

import java.lang.annotation.*;

/**
 * Indicates that annotated {@link JdbcDatabaseContainer} should be used by extension
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerJdbc {}
