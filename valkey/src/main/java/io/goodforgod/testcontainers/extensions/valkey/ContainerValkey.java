package io.goodforgod.testcontainers.extensions.valkey;

import java.lang.annotation.*;

/**
 * Indicates that annotated field containers {@link ValkeyContainer} instance
 * that should be used by {@link TestcontainersValkey} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerValkey {}
