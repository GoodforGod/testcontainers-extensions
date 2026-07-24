package io.goodforgod.testcontainers.extensions.redpanda;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.Isolation;
import io.goodforgod.testcontainers.extensions.Network;
import io.goodforgod.testcontainers.extensions.TestcontainersOrchestratorExtension;
import java.lang.annotation.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.redpanda.RedpandaContainer;

/**
 * Extension that is running {@link RedpandaContainer} for tests in different modes with database
 * schema migration support between test executions
 */
@Order(Order.DEFAULT - 100) // Run before other extensions
@ExtendWith(TestcontainersOrchestratorExtension.class)
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface TestcontainersRedpanda {

    /**
     * @return Redpanda image
     *             <p>
     *             1) Image can have static value: "redpandadata/redpanda:v25.1.11"
     *             2) Image can be provided via environment variable using syntax: "${MY_IMAGE_ENV}"
     *             3) Image environment variable can have default value if empty using syntax:
     *             "${MY_IMAGE_ENV|redpandadata/redpanda:v25.1.11}"
     */
    String image() default "redpandadata/redpanda:v25.1.11";

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

    /**
     * @return topics to set up right after container started
     */
    Topics topics() default @Topics;
}
