package io.goodforgod.testcontainers.extensions.cassandra;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.Network;
import java.lang.annotation.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.CassandraContainer;

/**
 * Extension that is running {@link CassandraContainer} for tests in different modes with
 * database
 * schema migration support between test executions
 */
@Order(Order.DEFAULT - 100) // Run before other extensions
@ExtendWith(TestcontainersCassandraExtension.class)
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface TestcontainersCassandra {

    /**
     * @return Cassandra image
     *             <p>
     *             1) Image can have static value: "cassandra:4.1"
     *             2) Image can be provided via environment variable using syntax: "${MY_IMAGE_ENV}"
     *             3) Image environment variable can have default value if empty using syntax:
     *             "${MY_IMAGE_ENV|cassandra:4.1}"
     *             <p>
     */
    String image() default "cassandra:4.1";

    /**
     * @return when to start container
     */
    ContainerMode mode() default ContainerMode.PER_METHOD;

    /**
     * @return container network details
     */
    Network network() default @Network(shared = false);

    Migration migration() default @Migration(engine = Migration.Engines.SCRIPTS,
            apply = Migration.Mode.NONE,
            drop = Migration.Mode.NONE,
            locations = {});
}
