package io.goodforgod.testcontainers.extensions.rabbitmq;

import java.lang.annotation.*;

@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Topology {

    enum Mode {
        NONE,
        PER_CLASS,
        PER_METHOD,
    }

    Queue[] queues() default {};

    Exchange[] exchanges() default {};

    Binding[] bindings() default {};

    Mode reset() default Mode.NONE;
}
