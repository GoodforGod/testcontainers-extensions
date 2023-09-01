package io.goodforgod.testcontainers.extensions.jdbc;

import org.testcontainers.containers.MySQLContainer;

import java.lang.annotation.*;

/**
 * Indicates that annotated field containers {@link MySQLContainer} instance
 * that should be used by {@link TestcontainersMysql} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerMysql {}
