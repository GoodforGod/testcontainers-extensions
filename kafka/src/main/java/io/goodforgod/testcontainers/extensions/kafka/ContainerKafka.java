package io.goodforgod.testcontainers.extensions.kafka;

import java.lang.annotation.*;

/**
 * Indicates that annotated field containers {@link KafkaContainerExtra} instance
 * that should be used by {@link TestcontainersKafka} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerKafka {}
