package io.goodforgod.testcontainers.extensions.kafka;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.Network;
import java.lang.annotation.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Extension that is running {@link KafkaContainerExtra} for tests in different modes with database
 * schema migration support between test executions
 */
@Order(Order.DEFAULT - 100) // Run before other extensions
@ExtendWith(TestcontainersKafkaExtension.class)
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface TestcontainersKafka {

    /**
     * @see TestcontainersKafkaExtension#getContainerDefault(KafkaMetadata)
     * @return Kafka image
     *             <p>
     *             1) Image can have static value: "confluentinc/cp-kafka:7.4.1"
     *             2) Image can be provided via environment variable using syntax: "${MY_IMAGE_ENV}"
     *             3) Image environment variable can have default value if empty using syntax:
     *             "${MY_IMAGE_ENV|confluentinc/cp-kafka:7.4.1}"
     *             <p>
     */
    String image() default "confluentinc/cp-kafka:7.4.1";

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
