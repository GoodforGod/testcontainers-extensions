package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.*;

/**
 * Indicates that annotated field or parameter should be injected with {@link JdbcConnection}
 */
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerJdbcConnection {}
