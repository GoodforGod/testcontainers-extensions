package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.Network;
import java.lang.annotation.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.OracleContainer;

/**
 * Extension that is running {@link OracleContainer} for tests in different modes with database
 * schema migration support between test executions
 */
@Order(Order.DEFAULT - 100) // Run before other extensions
@ExtendWith(TestcontainersOracleExtension.class)
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface TestcontainersOracle {

    /**
     * @return Oracle image
     *             <p>
     *             1) Image can have static value: "gvenzl/oracle-xe:18.4.0-slim-faststart"
     *             2) Image can be provided via environment variable using syntax: "${MY_IMAGE_ENV}"
     *             3) Image environment variable can have default value if empty using syntax:
     *             "${MY_IMAGE_ENV|gvenzl/oracle-xe:18.4.0-slim-faststart}"
     */
    String image() default "gvenzl/oracle-xe:18.4.0-slim-faststart";

    /**
     * @return when to start container
     */
    ContainerMode mode() default ContainerMode.PER_METHOD;

    /**
     * @return container network details
     */
    Network network() default @Network(shared = false);

    Migration migration() default @Migration(engine = Migration.Engines.FLYWAY,
            apply = Migration.Mode.NONE,
            drop = Migration.Mode.NONE);
}
