package io.goodforgod.testcontainers.extensions.mockserver;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.Network;
import java.lang.annotation.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.MockServerContainer;

/**
 * Extension that is running {@link MockServerContainer} for tests in different modes with
 * database
 * schema migration support between test executions
 */
@Order(Order.DEFAULT - 100) // Run before other extensions
@ExtendWith(TestcontainersMockServerExtension.class)
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface TestcontainersMockServer {

    /**
     * @return MockServer image
     *             <p>
     *             1) Image can have static value: "mockserver/mockserver:5.15.0"
     *             2) Image can be provided via environment variable using syntax: "${MY_IMAGE_ENV}"
     *             3) Image environment variable can have default value if empty using syntax:
     *             "${MY_IMAGE_ENV|mockserver/mockserver:5.15.0}"
     */
    String image() default "mockserver/mockserver:5.15.0";

    /**
     * @return when to start container
     */
    ContainerMode mode() default ContainerMode.PER_METHOD;

    /**
     * @return container network details
     */
    Network network() default @Network(shared = false);
}
