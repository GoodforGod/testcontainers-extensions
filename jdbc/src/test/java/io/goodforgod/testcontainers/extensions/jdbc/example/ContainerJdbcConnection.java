package io.goodforgod.testcontainers.extensions.jdbc.example;

import io.goodforgod.testcontainers.extensions.jdbc.JdbcConnection;
import java.lang.annotation.*;

/**
 * Indicates that annotated field or parameter should be injected with {@link JdbcConnection} value
 * of current active container
 */
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerJdbcConnection {}
