package io.goodforgod.testcontainers.extensions.redpanda;

import java.lang.annotation.*;
import org.testcontainers.redpanda.RedpandaContainer;

/**
 * Indicates that annotated field containers {@link RedpandaContainer} instance
 * that should be used by {@link TestcontainersRedpanda} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerRedpanda {}
