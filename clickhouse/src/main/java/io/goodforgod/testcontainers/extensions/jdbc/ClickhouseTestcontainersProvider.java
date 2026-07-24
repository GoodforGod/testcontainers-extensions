package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.Annotation;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.clickhouse.ClickHouseContainer;

@Internal
public final class ClickhouseTestcontainersProvider extends
        AbstractJdbcTestcontainersProvider<TestcontainersClickhouse, ClickHouseContainer> {

    private final TestcontainersClickhouseExtension delegate = new TestcontainersClickhouseExtension();

    @Override
    public @NotNull Class<TestcontainersClickhouse> annotationType() {
        return TestcontainersClickhouse.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerClickhouse.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionClickhouse.class;
    }

    @Override
    protected AbstractTestcontainersJdbcExtension<ClickHouseContainer, JdbcMetadata> delegate() {
        return delegate;
    }

    @Override
    protected JdbcMetadata metadata(@NotNull TestcontainersClickhouse annotation) {
        return metadata(annotation.network(), annotation.image(), annotation.mode(), annotation.migration());
    }

    @Override
    protected Migration migration(@NotNull TestcontainersClickhouse annotation) {
        return annotation.migration();
    }
}
