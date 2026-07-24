package io.goodforgod.testcontainers.extensions.orchestrator.minio;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.Network;
import io.goodforgod.testcontainers.extensions.TestcontainersOrchestratorExtension;
import java.lang.annotation.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;

@Order(Order.DEFAULT - 100)
@ExtendWith(TestcontainersOrchestratorExtension.class)
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface TestcontainersMinio {

    String image() default "minio:fake";

    boolean dependsOnRedis() default false;

    ContainerMode mode() default ContainerMode.PER_CLASS;

    Network network() default @Network(shared = false);
}
