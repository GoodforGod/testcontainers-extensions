package io.goodforgod.testcontainers.extensions.minio;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.Isolation;
import io.goodforgod.testcontainers.extensions.Network;
import io.goodforgod.testcontainers.extensions.TestcontainersOrchestratorExtension;
import java.lang.annotation.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.MinIOContainer;

/**
 * Extension that is running {@link MinIOContainer} for tests in different modes with
 * database
 * schema migration support between test executions
 */
@Order(Order.DEFAULT - 100) // Run before other extensions
@ExtendWith(TestcontainersOrchestratorExtension.class)
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface TestcontainersMinio {

    /**
     * @return MinIOContainer image
     *             <p>
     *             1) Image can have static value:
     *             "pgsty/minio:RELEASE.2026-08-04T00-00-00Z"
     *             2) Image can be provided via environment variable using syntax: "${MY_IMAGE_ENV}"
     *             3) Image environment variable can have default value if empty using syntax:
     *             "${MY_IMAGE_ENV|pgsty/minio:RELEASE.2026-08-04T00-00-00Z}"
     *             <p>
     *             MinIO no longer publishes images to Docker Hub, images are now available at quay.io
     */
    String image() default "pgsty/minio:RELEASE.2026-08-04T00-00-00Z";

    /**
     * @return when to start container
     */
    ContainerMode mode() default ContainerMode.PER_METHOD;

    /**
     * @return container network details
     */
    Network network() default @Network(shared = false);

    /**
     * @return logical connection isolation mode. Disabled by default and preserves regular connection
     *             behavior.
     */
    Isolation isolation() default @Isolation;

    Bucket bucket() default @Bucket(value = {}, create = Bucket.Mode.NONE, drop = Bucket.Mode.NONE);
}
