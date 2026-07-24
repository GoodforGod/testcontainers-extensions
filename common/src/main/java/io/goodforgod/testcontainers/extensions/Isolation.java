package io.goodforgod.testcontainers.extensions;

import java.lang.annotation.*;

// @formatter:off
/**
 * Controls logical connection isolation inside a started container.
 * <p>
 * {@link Mode#DISABLED} is the default and keeps the historical behavior: every injected
 * connection points to the regular container connection and no additional lifecycle restrictions
 * are applied.
 * <p>
 * {@link Mode#PER_METHOD} creates a logical namespace for every test method while reusing the
 * same physical container according to {@link ContainerMode}. Providers choose the concrete
 * namespace type, for example database, keyspace, bucket, or another service-specific namespace.
 * Field injection is supported only with JUnit's default {@code TestInstance.Lifecycle.PER_METHOD}
 * lifecycle. {@code TestInstance.Lifecycle.PER_CLASS}, constructor injection, and {@code @BeforeAll}
 * parameter injection are rejected because one test instance or lifecycle phase could
 * otherwise observe a connection scoped to another test method.
 * <p>
 * Example:
 *
 * <pre>{@code
 * @TestcontainersPostgreSQL(
 *         mode = ContainerMode.PER_RUN,
 *         isolation = @Isolation(Isolation.Mode.PER_METHOD))
 * class RepositoryTests {
 *
 *     @ConnectionPostgreSQL
 *     JdbcConnection connection;
 * }
 * }
 * </pre>
 */
// @formatter:on
@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Isolation {

    /**
     * @return logical connection isolation mode
     */
    Mode value() default Mode.DISABLED;

    enum Mode {
        /**
         * Keeps the default non-isolated connection behavior.
         */
        DISABLED,
        /**
         * Creates a separate logical connection namespace for every test method.
         * Use with conjunction with {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_METHOD}
         * to achieve parallel text execution support
         */
        PER_METHOD
    }
}
