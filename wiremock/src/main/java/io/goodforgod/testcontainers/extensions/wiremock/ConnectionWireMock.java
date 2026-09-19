package io.goodforgod.testcontainers.extensions.wiremock;

import java.lang.annotation.*;

/**
 * Indicates that annotated field or parameter should be injected with {@link WireMockConnection}
 * value of current active container
 */
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConnectionWireMock {}
