package io.goodforgod.testcontainers.extensions.rustfs;

import java.lang.annotation.*;

/**
 * Indicates that annotated field or parameter should be injected with {@link RustFSConnection}
 * value of current active container
 */
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConnectionRustFS {}
