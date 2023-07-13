package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.*;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Indicates that already instantiated annotated field with type {@link PostgreSQLContainer}
 * should be used by extension rather than creating default one
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerPostgres {

}
