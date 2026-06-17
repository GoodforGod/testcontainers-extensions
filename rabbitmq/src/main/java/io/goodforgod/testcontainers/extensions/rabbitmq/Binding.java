package io.goodforgod.testcontainers.extensions.rabbitmq;

import java.lang.annotation.*;

@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Binding {

    String queue();

    String exchange();

    String routingKey() default "";

    String[] arguments() default {};
}
