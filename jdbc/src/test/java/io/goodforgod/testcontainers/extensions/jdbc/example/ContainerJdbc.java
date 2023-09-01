package io.goodforgod.testcontainers.extensions.jdbc.example;

import org.testcontainers.containers.JdbcDatabaseContainer;

import java.lang.annotation.*;

/**
 * Indicates that already instantiated annotated field with type {@link JdbcDatabaseContainer}
 * should be used by extension rather than creating default one
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerJdbc {}
