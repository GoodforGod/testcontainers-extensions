package io.goodforgod.testcontainers.extensions.arangodb;

import java.lang.annotation.*;

/**
 * Field or parameter annotation for ArangoDB connection.
 */
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConnectionArango {}
