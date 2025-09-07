package io.goodforgod.testcontainers.extensions.scylla;

import java.lang.annotation.*;
import org.testcontainers.scylladb.ScyllaDBContainer;

/**
 * Indicates that annotated field containers {@link ScyllaDBContainer} instance
 * that should be used by {@link TestcontainersScylla} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerScylla {}
