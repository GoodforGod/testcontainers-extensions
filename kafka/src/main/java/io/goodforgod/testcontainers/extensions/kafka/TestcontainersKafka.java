package io.goodforgod.testcontainers.extensions.kafka;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.lang.annotation.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.KafkaContainer;

/**
 * Extension that is running {@link KafkaContainer} for tests in different modes with database
 * schema migration support between test executions
 */
@Order(Order.DEFAULT - 100) // Run before other extensions
@ExtendWith(TestcontainersKafkaExtension.class)
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface TestcontainersKafka {

    /**
     * @return where nether to create default container with
     *             {@link org.testcontainers.containers.Network#SHARED} network
     */
    boolean network() default false;

    /**
     * @return Kafka image
     */
    String image() default "confluentinc/cp-kafka:7.4.1";

    /**
     * @return when to start container
     */
    ContainerMode mode() default ContainerMode.PER_METHOD;

    /**
     * @return topics to set up right after container started
     */
    Topics topics() default @Topics;
}
