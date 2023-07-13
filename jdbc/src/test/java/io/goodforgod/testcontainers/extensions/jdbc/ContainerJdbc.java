package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.*;
import org.testcontainers.containers.JdbcDatabaseContainer;

/**
 * Indicates that already instantiated annotated field with type {@link JdbcDatabaseContainer}
 * should be used by extension rather than creating default one
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerJdbc {}
