package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.*;

/**
 * Indicates that annotated field containers {@link org.testcontainers.containers.MySQLContainer}
 * instance
 * that should be used by {@link TestcontainersMySQL} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerMySQL {}
