package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.*;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Indicates that annotated field containers {@link PostgreSQLContainer} instance
 * that should be used by {@link TestcontainersPostgres} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerPostgres {}
