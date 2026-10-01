package io.goodforgod.testcontainers.extensions.wiremock;

import java.lang.annotation.*;
import org.wiremock.integrations.testcontainers.WireMockContainer;

/**
 * Indicates that annotated field containers {@link WireMockContainer} instance
 * that should be used by {@link TestcontainersWireMock} rather than creating default container
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerWireMock {}
