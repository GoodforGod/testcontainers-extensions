package io.goodforgod.testcontainers.extensions.example;

import java.lang.annotation.*;

/**
 * Indicates that annotated field containers {@link RedisContainer} instance
 * that should be used by {@link TestcontainersRedis} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerRedis {}
