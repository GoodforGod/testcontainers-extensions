package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.Annotation;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.PostgreSQLContainer;

@Internal
public final class PostgreSQLTestcontainersProvider extends
        AbstractJdbcTestcontainersProvider<TestcontainersPostgreSQL, PostgreSQLContainer<?>> {

    private final TestcontainersPostgreSQLExtension delegate = new TestcontainersPostgreSQLExtension();

    @Override
    public @NotNull Class<TestcontainersPostgreSQL> annotationType() {
        return TestcontainersPostgreSQL.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerPostgreSQL.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionPostgreSQL.class;
    }

    @Override
    protected AbstractTestcontainersJdbcExtension<PostgreSQLContainer<?>, JdbcMetadata> delegate() {
        return delegate;
    }

    @Override
    protected JdbcMetadata metadata(@NotNull TestcontainersPostgreSQL annotation) {
        return metadata(annotation.network(), annotation.image(), annotation.mode(), annotation.migration());
    }

    @Override
    protected Migration migration(@NotNull TestcontainersPostgreSQL annotation) {
        return annotation.migration();
    }
}
