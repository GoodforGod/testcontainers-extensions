package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.*;
import org.testcontainers.containers.MariaDBContainer;

/**
 * Indicates that annotated field containers {@link MariaDBContainer} instance
 * that should be used by {@link TestcontainersMariadb} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerMariadb {}
