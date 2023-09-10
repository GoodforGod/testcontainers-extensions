package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.*;

/**
 * Indicates that annotated field containers {@link MariaDBContainerExtra} instance
 * that should be used by {@link TestcontainersMariadb} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerMariadb {}
