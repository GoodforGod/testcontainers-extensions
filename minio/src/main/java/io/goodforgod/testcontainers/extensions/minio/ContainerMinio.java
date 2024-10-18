package io.goodforgod.testcontainers.extensions.minio;

import java.lang.annotation.*;

/**
 * Indicates that annotated field containers {@link org.testcontainers.containers.MinIOContainer}
 * instance
 * that should be used by {@link TestcontainersMinio} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerMinio {}
