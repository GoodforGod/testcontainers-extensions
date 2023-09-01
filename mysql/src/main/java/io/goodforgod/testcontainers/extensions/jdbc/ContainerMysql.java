package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.*;
import org.testcontainers.containers.MySQLContainer;

/**
 * Indicates that annotated field containers {@link MySQLContainer} instance
 * that should be used by {@link TestcontainersMysql} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerMysql {}
