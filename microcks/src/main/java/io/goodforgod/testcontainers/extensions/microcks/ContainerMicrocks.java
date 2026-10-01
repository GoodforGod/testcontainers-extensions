package io.goodforgod.testcontainers.extensions.microcks;

import io.github.microcks.testcontainers.MicrocksContainer;
import java.lang.annotation.*;

/**
 * Indicates that annotated field containers {@link MicrocksContainer} instance
 * that should be used by {@link TestcontainersMicrocks} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerMicrocks {}
