package io.goodforgod.testcontainers.extensions.orchestrator.redis;

import java.lang.annotation.*;

@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerRedis {}
