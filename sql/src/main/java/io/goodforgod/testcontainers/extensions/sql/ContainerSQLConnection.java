package io.goodforgod.testcontainers.extensions.sql;

import java.lang.annotation.*;

@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerSQLConnection {}
