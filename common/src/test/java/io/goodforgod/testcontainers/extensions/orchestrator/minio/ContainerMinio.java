package io.goodforgod.testcontainers.extensions.orchestrator.minio;

import java.lang.annotation.*;

@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerMinio {}
