package io.goodforgod.testcontainers.extensions.rabbitmq;

import java.lang.annotation.*;

@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Exchange {

    enum Type {
        DIRECT,
        FANOUT,
        TOPIC,
        HEADERS,
    }

    String name();

    Type type() default Type.DIRECT;

    boolean durable() default false;

    boolean autoDelete() default false;

    boolean internal() default false;

    String[] arguments() default {};
}