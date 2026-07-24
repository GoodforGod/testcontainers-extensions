package io.goodforgod.testcontainers.extensions.rabbitmq;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.Isolation;
import io.goodforgod.testcontainers.extensions.Network;
import io.goodforgod.testcontainers.extensions.TestcontainersOrchestratorExtension;
import java.lang.annotation.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Extension that is running {@link RabbitMQContainer} for tests in different modes with topology
 * setup support between test executions
 */
@Order(Order.DEFAULT - 100)
@ExtendWith(TestcontainersOrchestratorExtension.class)
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface TestcontainersRabbitMQ {

    /**
     * @return RabbitMQ image
     *             <p>
     *             1) Image can have static value: "rabbitmq:3.13-management-alpine"
     *             2) Image can be provided via environment variable using syntax: "${MY_IMAGE_ENV}"
     *             3) Image environment variable can have default value if empty using syntax:
     *             "${MY_IMAGE_ENV|rabbitmq:3.13-management-alpine}"
     */
    String image() default "rabbitmq:3.13-management-alpine";

    ContainerMode mode() default ContainerMode.PER_METHOD;

    Network network() default @Network(shared = false);

    /**
     * @return logical connection isolation mode. Disabled by default and preserves regular connection
     *             behavior.
     */
    Isolation isolation() default @Isolation;

    Topology topology() default @Topology;
}
