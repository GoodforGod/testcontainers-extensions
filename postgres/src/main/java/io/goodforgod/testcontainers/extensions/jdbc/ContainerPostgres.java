package io.goodforgod.testcontainers.extensions.jdbc;

import org.testcontainers.containers.PostgreSQLContainer;

import java.lang.annotation.*;

/**
 * Indicates that annotated field containers {@link PostgreSQLContainer} instance
 * that should be used by {@link TestcontainersPostgres} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerPostgres {}
