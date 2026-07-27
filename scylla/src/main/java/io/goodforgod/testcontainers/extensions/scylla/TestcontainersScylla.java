package io.goodforgod.testcontainers.extensions.scylla;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.Isolation;
import io.goodforgod.testcontainers.extensions.Network;
import io.goodforgod.testcontainers.extensions.TestcontainersOrchestratorExtension;
import java.lang.annotation.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.scylladb.ScyllaDBContainer;

/**
 * Extension that is running {@link ScyllaDBContainer} for tests in different modes with
 * database
 * schema migration support between test executions
 */
@Order(Order.DEFAULT - 100) // Run before other extensions
@ExtendWith(TestcontainersOrchestratorExtension.class)
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface TestcontainersScylla {

    /**
     * @return Scylla image
     *             <p>
     *             1) Image can have static value: "scylladb/scylla:2025.3"
     *             2) Image can be provided via environment variable using syntax: "${MY_IMAGE_ENV}"
     *             3) Image environment variable can have default value if empty using syntax:
     *             "${MY_IMAGE_ENV|scylladb/scylla:2025.3}"
     *             <p>
     */
    String image() default "scylladb/scylla:2025.3";

    /**
     * @return when to start container
     */
    ContainerMode mode() default ContainerMode.PER_METHOD;

    /**
     * @return container network details
     */
    Network network() default @Network(shared = false);

    /**
     * @return logical connection isolation mode. Disabled by default and preserves regular connection
     *             behavior.
     */
    Isolation isolation() default @Isolation;

    Migration migration() default @Migration(engine = Migration.Engines.SCRIPTS,
            apply = Migration.Mode.NONE,
            drop = Migration.Mode.NONE,
            locations = {});
}
