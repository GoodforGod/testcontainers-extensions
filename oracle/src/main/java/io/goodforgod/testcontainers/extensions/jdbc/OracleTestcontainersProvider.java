package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.Isolation;
import java.lang.annotation.Annotation;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.OracleContainer;

@Internal
public final class OracleTestcontainersProvider extends
        AbstractJdbcTestcontainersProvider<TestcontainersOracle, OracleContainer> {

    private final TestcontainersOracleExtension delegate = new TestcontainersOracleExtension();

    @Override
    public @NotNull Class<TestcontainersOracle> annotationType() {
        return TestcontainersOracle.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerOracle.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionOracle.class;
    }

    @Override
    protected AbstractTestcontainersJdbcExtension<OracleContainer, JdbcMetadata> delegate() {
        return delegate;
    }

    @Override
    protected JdbcMetadata metadata(@NotNull TestcontainersOracle annotation) {
        return metadata(annotation.network(), annotation.image(), annotation.mode(), annotation.migration());
    }

    @Override
    protected Migration migration(@NotNull TestcontainersOracle annotation) {
        return annotation.migration();
    }

    @Override
    protected Isolation isolationAnnotation(@NotNull TestcontainersOracle annotation) {
        return annotation.isolation();
    }

    @Override
    protected boolean isIsolationSupported(TestcontainersOracle annotation) {
        return false;
    }
}
