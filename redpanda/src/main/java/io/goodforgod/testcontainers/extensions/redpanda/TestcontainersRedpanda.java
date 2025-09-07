package io.goodforgod.testcontainers.extensions.redpanda;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.Network;
import java.lang.annotation.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.redpanda.RedpandaContainer;

/**
 * Extension that is running {@link RedpandaContainer} for tests in different modes with database
 * schema migration support between test executions
 */
@Order(Order.DEFAULT - 100) // Run before other extensions
@ExtendWith(TestcontainersRedpandaExtension.class)
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
     * @return topics to set up right after container started
     */
    Topics topics() default @Topics;
}
