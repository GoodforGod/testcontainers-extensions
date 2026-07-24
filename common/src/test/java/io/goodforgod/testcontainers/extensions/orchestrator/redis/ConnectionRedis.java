package io.goodforgod.testcontainers.extensions.orchestrator.redis;

import java.lang.annotation.*;

@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConnectionRedis {}
