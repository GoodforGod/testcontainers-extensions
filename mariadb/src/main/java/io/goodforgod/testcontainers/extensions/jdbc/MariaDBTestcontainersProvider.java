package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.Annotation;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.MariaDBContainer;

@Internal
public final class MariaDBTestcontainersProvider extends
        AbstractJdbcTestcontainersProvider<TestcontainersMariaDB, MariaDBContainer<?>> {

    private final TestcontainersMariaDBExtension delegate = new TestcontainersMariaDBExtension();

    @Override
    public @NotNull Class<TestcontainersMariaDB> annotationType() {
        return TestcontainersMariaDB.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerMariaDB.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionMariaDB.class;
    }

    @Override
    protected AbstractTestcontainersJdbcExtension<MariaDBContainer<?>, JdbcMetadata> delegate() {
        return delegate;
    }

    @Override
    protected JdbcMetadata metadata(@NotNull TestcontainersMariaDB annotation) {
        return metadata(annotation.network(), annotation.image(), annotation.mode(), annotation.migration());
    }

    @Override
    protected Migration migration(@NotNull TestcontainersMariaDB annotation) {
        return annotation.migration();
    }
}
