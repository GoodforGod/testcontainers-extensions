package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.*;

/**
 * Indicates that annotated field containers {@link PostgreSQLContainerExtra} instance
 * that should be used by {@link TestcontainersPostgres} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerPostgres {}
