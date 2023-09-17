package io.goodforgod.testcontainers.extensions.mockserver;

import java.lang.annotation.*;

/**
 * Indicates that annotated field containers {@link MockServerContainerExtra} instance
 * that should be used by {@link TestcontainersMockserver} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerMockserver {}
