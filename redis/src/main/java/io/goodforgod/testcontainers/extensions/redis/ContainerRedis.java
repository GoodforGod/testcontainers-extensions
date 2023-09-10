package io.goodforgod.testcontainers.extensions.redis;

import java.lang.annotation.*;

/**
 * Indicates that annotated field containers {@link RedisContainerExtra} instance
 * that should be used by {@link TestcontainersRedis} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerRedis {}
