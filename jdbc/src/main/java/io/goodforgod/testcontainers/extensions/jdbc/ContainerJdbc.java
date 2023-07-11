package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.*;

@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerJdbc {}
