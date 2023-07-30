package io.goodforgod.testcontainers.extensions.cassandra;

import java.lang.annotation.*;

/**
 * Indicates that annotated field or parameter should be injected with {@link CassandraConnection}
 * value
 * of current active container
 */
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerCassandraConnection {}
