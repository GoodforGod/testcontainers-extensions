package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.*;

@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerJdbcConnection {}
