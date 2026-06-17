package io.goodforgod.testcontainers.extensions.rabbitmq;

import java.lang.annotation.*;

/**
 * Indicates that annotated field or parameter should be injected with {@link RabbitMQConnection}
 * value
 * of current active container
 */
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConnectionRabbitMQ {

    /**
     * @return {@link RabbitMQConnection} properties that will be used for connection factory,
     *             publishers and consumers
     */
    String[] properties() default {};
}
