package io.goodforgod.testcontainers.extensions.scylla;

import java.lang.annotation.*;

/**
 * Indicates that annotated field or parameter should be injected with {@link ScyllaConnection}
 * value
 * of current active container
 */
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConnectionScylla {}
