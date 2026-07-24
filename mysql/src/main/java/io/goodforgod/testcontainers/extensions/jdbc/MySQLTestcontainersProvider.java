package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.Isolation;
import java.lang.annotation.Annotation;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.MySQLContainer;

@Internal
public final class MySQLTestcontainersProvider extends
        AbstractJdbcTestcontainersProvider<TestcontainersMySQL, MySQLContainer<?>> {

    private final TestcontainersMySQLExtension delegate = new TestcontainersMySQLExtension();

    @Override
    public @NotNull Class<TestcontainersMySQL> annotationType() {
        return TestcontainersMySQL.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerMySQL.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionMySQL.class;
    }

    @Override
    protected AbstractTestcontainersJdbcExtension<MySQLContainer<?>, JdbcMetadata> delegate() {
        return delegate;
    }

    @Override
    protected JdbcMetadata metadata(@NotNull TestcontainersMySQL annotation) {
        return metadata(annotation.network(), annotation.image(), annotation.mode(), annotation.migration());
    }

    @Override
    protected Migration migration(@NotNull TestcontainersMySQL annotation) {
        return annotation.migration();
    }

    @Override
    protected Isolation isolationAnnotation(@NotNull TestcontainersMySQL annotation) {
        return annotation.isolation();
    }
}
