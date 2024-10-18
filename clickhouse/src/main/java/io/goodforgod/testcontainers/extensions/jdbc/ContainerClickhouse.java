package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.*;
import org.testcontainers.clickhouse.ClickHouseContainer;

/**
 * Indicates that annotated field containers {@link ClickHouseContainer} instance
 * that should be used by {@link TestcontainersClickhouse} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerClickhouse {

}
