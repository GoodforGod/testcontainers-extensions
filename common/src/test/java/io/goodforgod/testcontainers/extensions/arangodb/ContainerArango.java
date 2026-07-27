package io.goodforgod.testcontainers.extensions.arangodb;

import java.lang.annotation.*;

/**
 * Field annotation for custom ArangoDB container.
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerArango {}
