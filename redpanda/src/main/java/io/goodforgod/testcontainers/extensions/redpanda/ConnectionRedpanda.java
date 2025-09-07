package io.goodforgod.testcontainers.extensions.redpanda;

import java.lang.annotation.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

/**
 * Indicates that annotated field or parameter should be injected with {@link RedpandaConnection}
 * value
 * of current active container
 */
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConnectionRedpanda {

    /**
     * @return {@link RedpandaConnection} properties that will be used {@link ConsumerConfig} and
     *             {@link ProducerConfig}
     */
    String[] properties() default {};
}
