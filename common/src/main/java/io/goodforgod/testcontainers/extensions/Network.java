package io.goodforgod.testcontainers.extensions;

import java.lang.annotation.*;

/**
 * Describes container network details for {@code @Testcontainers...} annotations.
 *
 * @see ContainerNetwork
 */
@Deprecated
@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Network {

    /**
     * @return if true then run container in {@link org.testcontainers.containers.Network#SHARED}
     *             network
     */
    boolean shared();

    /**
     * @return extra network alias except the default ones (depends on extension implementation)
     *             <p>
     *             1) Alias can have static value: "my_alias_value"
     *             2) Alias can be provided via environment variable using syntax: "${MY_ALIAS_ENV}"
     *             3) Alias environment variable can have default value if empty using syntax:
     *             "${MY_ALIAS_ENV|my_alias_default}"
     *             <p>
     *             Default alias is always created despite this property and will be used if property is
     *             empty or missing for network connection parameters
     */
    String alias() default "";
}
