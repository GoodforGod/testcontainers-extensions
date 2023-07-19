package io.goodforgod.testcontainers.extensions.kafka;

import java.lang.annotation.*;
import org.testcontainers.containers.KafkaContainer;

/**
 * Indicates that annotated field containers {@link KafkaContainer} instance
 * that should be used by {@link TestcontainersKafka} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerKafka {}
