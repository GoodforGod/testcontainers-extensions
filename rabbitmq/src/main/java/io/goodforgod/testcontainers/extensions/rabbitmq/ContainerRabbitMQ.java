package io.goodforgod.testcontainers.extensions.rabbitmq;

import java.lang.annotation.*;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Indicates that annotated field contains {@link RabbitMQContainer} instance
 * that should be used by {@link TestcontainersRabbitMQ} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerRabbitMQ {}
