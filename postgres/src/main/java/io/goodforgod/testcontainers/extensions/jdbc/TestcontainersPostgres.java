package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Extension annotation that is used to extend {@link PostgreSQLContainer} capabilities
 */
@Order(Order.DEFAULT - 100) // Run before other extensions
@ExtendWith(TestcontainersPostgresExtension.class)
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface TestcontainersPostgres {

    /**
     * @return Postgres image
     */
    String image() default "postgres:15.3-alpine";

    /**
     * @return when to start container
     */
    ContainerMode mode() default ContainerMode.PER_METHOD;

    Migration migration() default @Migration(engine = Migration.Engines.FLYWAY,
            apply = Migration.Mode.NONE,
            drop = Migration.Mode.NONE);
}
