package io.goodforgod.testcontainers.extensions.rabbitmq;

import java.lang.annotation.*;

@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Queue {

    String name();

    boolean durable() default false;

    boolean exclusive() default false;

    boolean autoDelete() default false;

    String[] arguments() default {};
}
