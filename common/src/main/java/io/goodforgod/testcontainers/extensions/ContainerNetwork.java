package io.goodforgod.testcontainers.extensions;

import java.lang.annotation.*;

/**
 * Marks a test class field of type {@link org.testcontainers.containers.Network} that should be
 * used by containers started by the shared orchestrator.
 * <p>
 * Put this annotation on a static or instance field when several {@code @Testcontainers...}
 * services must use the same custom Testcontainers network:
 *
 * <pre>{@code
 * @TestcontainersRedis
 * @TestcontainersMinio
 * class StorageTests {
 *
 *    @ContainerNetwork
 *    private static final org.testcontainers.containers.Network NETWORK = Network.SHARED;
 * }
 * }
 * </pre>
 * <p>
 * A {@code @ContainerNetwork} field has priority over {@link Network#shared()}. Network aliases are
 * still taken from each service annotation.
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerNetwork {
}
