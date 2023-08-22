package io.goodforgod.testcontainers.extensions.example;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.lang.annotation.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Extension that is running {@link RedisContainer} for tests in different modes with database
 * schema migration support between test executions
 */
@Order(Order.DEFAULT - 100) // Run before other extensions
@ExtendWith(TestcontainersRedisExtension.class)
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface TestcontainersRedis {

    /**
     * @return where nether to create default container with
     *             {@link org.testcontainers.containers.Network#SHARED} network
     */
    boolean network() default false;

    /**
     * @return Redis image
     */
    String image() default "redis:7.2-alpine";

    /**
     * @return when to start container
     */
    ContainerMode mode() default ContainerMode.PER_METHOD;
}
