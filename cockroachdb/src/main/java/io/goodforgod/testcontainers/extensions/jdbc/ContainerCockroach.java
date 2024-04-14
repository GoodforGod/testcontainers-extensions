package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.*;
import org.testcontainers.containers.CockroachContainer;

/**
 * Indicates that annotated field containers {@link CockroachContainer} instance
 * that should be used by {@link TestcontainersCockroach} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerCockroach {

}
