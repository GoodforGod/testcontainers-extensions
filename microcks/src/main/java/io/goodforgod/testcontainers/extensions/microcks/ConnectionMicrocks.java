package io.goodforgod.testcontainers.extensions.microcks;

import java.lang.annotation.*;

/**
 * Indicates that annotated field or parameter should be injected with {@link MicrocksConnection}
 * value of current active container
 */
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConnectionMicrocks {}
