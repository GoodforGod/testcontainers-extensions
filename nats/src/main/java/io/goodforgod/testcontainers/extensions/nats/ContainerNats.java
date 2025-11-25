package io.goodforgod.testcontainers.extensions.nats;

import java.lang.annotation.*;

/**
 * Indicates that annotated field containers {@link io.testcontainers.nats.NatsContainer} instance
 * that should be used by {@link TestcontainersNats} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerNats {}
