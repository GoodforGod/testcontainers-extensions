package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.*;
import org.testcontainers.containers.OracleContainer;

/**
 * Indicates that annotated field containers {@link OracleContainer} instance
 * that should be used by {@link TestcontainersOracle} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerOracle {}
