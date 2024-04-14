package io.goodforgod.testcontainers.extensions.mockserver;

import java.lang.annotation.*;
import org.testcontainers.containers.MockServerContainer;

/**
 * Indicates that annotated field containers {@link MockServerContainer} instance
 * that should be used by {@link TestcontainersMockServer} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerMockServer {}
