package io.goodforgod.testcontainers.extensions.kafka;

import java.lang.annotation.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

/**
 * Indicates that annotated field or parameter should be injected with {@link KafkaConnection} value
 * of current active container
 */
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConnectionKafka {

    /**
     * @return {@link KafkaConnection} properties that will be used {@link ConsumerConfig} and
     *             {@link ProducerConfig}
     */
    String[] properties() default {};
}
