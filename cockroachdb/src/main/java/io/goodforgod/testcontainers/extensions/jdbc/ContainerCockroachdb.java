package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.*;

/**
 * Indicates that annotated field containers {@link CockroachContainerExtra} instance
 * that should be used by {@link TestcontainersCockroachdb} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerCockroachdb {}
