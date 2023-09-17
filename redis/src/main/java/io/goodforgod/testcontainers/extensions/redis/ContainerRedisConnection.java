package io.goodforgod.testcontainers.extensions.redis;

import java.lang.annotation.*;

/**
 * Indicates that annotated field or parameter should be injected with {@link RedisContainerExtra}
 * value
 * of current active container
 */
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerRedisConnection {}
