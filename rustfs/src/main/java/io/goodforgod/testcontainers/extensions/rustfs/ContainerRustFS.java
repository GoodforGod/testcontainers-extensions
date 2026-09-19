package io.goodforgod.testcontainers.extensions.rustfs;

import java.lang.annotation.*;

/**
 * Indicates that annotated field containers {@link RustFSContainer} instance
 * that should be used by {@link TestcontainersRustFS} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerRustFS {}
