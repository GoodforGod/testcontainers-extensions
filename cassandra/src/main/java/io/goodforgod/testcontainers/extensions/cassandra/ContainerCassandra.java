package io.goodforgod.testcontainers.extensions.cassandra;

import org.testcontainers.containers.CassandraContainer;

import java.lang.annotation.*;

/**
 * Indicates that annotated field containers {@link CassandraContainer} instance
 * that should be used by {@link TestcontainersCassandra} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerCassandra {}
