package io.goodforgod.testcontainers.extensions.sql;

import java.lang.annotation.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;

@Order(Order.DEFAULT - 100) // Run before other extensions
@ExtendWith(TestcontainersSQLExtension.class)
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@interface TestcontainersSQL {

    /**
     * @return Postgres image like: "postgres:15.3-alpine"
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
