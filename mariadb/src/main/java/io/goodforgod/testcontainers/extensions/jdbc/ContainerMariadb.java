package io.goodforgod.testcontainers.extensions.jdbc;

import org.testcontainers.containers.MariaDBContainer;

import java.lang.annotation.*;

/**
 * Indicates that annotated field containers {@link MariaDBContainer} instance
 * that should be used by {@link TestcontainersMariadb} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerMariadb {}
