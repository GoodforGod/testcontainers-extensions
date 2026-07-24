package io.goodforgod.testcontainers.extensions.arangodb;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.Isolation;
import io.goodforgod.testcontainers.extensions.Network;
import io.goodforgod.testcontainers.extensions.TestcontainersOrchestratorExtension;
import io.testcontainers.arangodb.containers.ArangoContainer;
import java.lang.annotation.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Extension that is running {@link ArangoContainer} for tests in different modes.
 */
@Order(Order.DEFAULT - 100) // Run before other extensions
@ExtendWith(TestcontainersOrchestratorExtension.class)
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface TestcontainersArango {

    /**
     * @return ArangoDB image
     *             <p>
     *             1) Image can have static value: "arangodb:3.12.4"
     *             2) Image can be provided via environment variable using syntax: "${MY_IMAGE_ENV}"
     *             3) Image environment variable can have default value if empty using syntax:
     *             "${MY_IMAGE_ENV|arangodb:3.12.4}"
     */
    String image() default "arangodb:3.12.4";

    /**
     * @return root user password. Empty value starts ArangoDB without authentication.
     */
    String password() default "";

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
}
