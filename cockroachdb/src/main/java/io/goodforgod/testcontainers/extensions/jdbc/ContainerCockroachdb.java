package io.goodforgod.testcontainers.extensions.jdbc;

import org.testcontainers.containers.CockroachContainer;

import java.lang.annotation.*;

/**
 * Indicates that annotated field containers {@link CockroachContainer} instance
 * that should be used by {@link TestcontainersCockroachdb} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerCockroachdb {}
