package io.goodforgod.testcontainers.extensions.kafka;

import java.lang.annotation.*;

/**
 * Indicates that annotated field or parameter should be injected with {@link KafkaConnection} value
 * of current active container
 */
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerKafkaConnection {

    /**
     * @return {@link KafkaConnection} properties that will be used for Producer & Consumer
     */
    Property[] properties() default {};

    @interface Property {

        String name();

        String value();
    }
}
