package io.goodforgod.testcontainers.extensions.nats;

import io.nats.client.Options;
import java.lang.annotation.*;
import java.util.Properties;

/**
 * Indicates that annotated field or parameter should be injected with {@link NatsConnection} value
 * of current active container
 */
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConnectionNats {

    /**
     * @return {@link NatsConnection} properties that will be used for
     *             {@link Options.Builder#properties(Properties)}}
     */
    String[] properties() default {};
}
