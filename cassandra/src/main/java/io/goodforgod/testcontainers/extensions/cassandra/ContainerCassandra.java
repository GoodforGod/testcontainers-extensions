package io.goodforgod.testcontainers.extensions.cassandra;

import java.lang.annotation.*;
import org.testcontainers.cassandra.CassandraContainer;

/**
 * Indicates that annotated field containers {@link CassandraContainer} instance
 * that should be used by {@link TestcontainersCassandra} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerCassandra {}
