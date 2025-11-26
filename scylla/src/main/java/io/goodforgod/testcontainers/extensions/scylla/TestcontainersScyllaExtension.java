package io.goodforgod.testcontainers.extensions.scylla;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.scylladb.ScyllaDBContainer;
import org.testcontainers.utility.DockerImageName;

@Internal
class TestcontainersScyllaExtension extends
        AbstractTestcontainersExtension<ScyllaConnection, ScyllaDBContainer, ScyllaMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersScyllaExtension.class);

    protected Class<ScyllaDBContainer> getContainerType() {
        return ScyllaDBContainer.class;
    }

    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerScylla.class;
    }

    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ConnectionScylla.class;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @Override
    protected Class<ScyllaConnection> getConnectionType() {
        return ScyllaConnection.class;
    }

    @Override
    protected ScyllaDBContainer createContainerDefault(ScyllaMetadata metadata) {
        var image = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("scylladb/scylla"));

        final ScyllaDBContainer container = new ScyllaDBContainer(image);
        final String alias = Optional.ofNullable(metadata.networkAlias())
                .orElseGet(() -> "scylla-" + System.currentTimeMillis());
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(ScyllaDBContainer.class))
                .withMdc("image", image.asCanonicalNameString())
                .withMdc("alias", alias));
        container.withStartupTimeout(Duration.ofMinutes(5));
        container.setNetworkAliases(new ArrayList<>(List.of(alias)));
        if (metadata.networkShared()) {
            container.withNetwork(Network.SHARED);
        }
        return container;
    }

    @Override
    protected ContainerContext<ScyllaConnection> createContainerContext(ScyllaDBContainer container) {
        return new ScyllaContext(container);
    }

    @NotNull
    protected Optional<ScyllaMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersScylla.class, context)
                .map(a -> new ScyllaMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(), a.migration()));
    }

    private void tryMigrateIfRequired(ScyllaMetadata metadata, ScyllaConnection connection) {
        ScyllaMigrationEngine migrationEngine = connection.migrationEngine(metadata.migration().engine());
        migrationEngine.apply(Arrays.asList(metadata.migration().locations()));
    }

    private void tryDropIfRequired(ScyllaMetadata metadata, ScyllaConnection connection) {
        ScyllaMigrationEngine migrationEngine = connection.migrationEngine(metadata.migration().engine());
        migrationEngine.drop(Arrays.asList(metadata.migration().locations()), metadata.migration().dropMode());
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        super.beforeAll(context);

        var metadata = getMetadata(context);
        if (metadata.migration().apply() == Migration.Mode.PER_CLASS) {
            var storage = getStorage(context);
            var connectionCurrent = getContainerContext(context).connection();
            tryMigrateIfRequired(metadata, connectionCurrent);
            storage.put(Migration.class, metadata.migration().apply());
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var metadata = getMetadata(context);
        if (metadata.runMode() == ContainerMode.PER_METHOD && metadata.migration().apply() == Migration.Mode.PER_CLASS) {
            throw new ExtensionConfigurationException(String.format(
                    "@%s can't apply migration in Migration.Mode.PER_CLASS mode when ContainerMode.PER_METHOD is used",
                    getContainerAnnotation().getSimpleName()));
        }

        super.beforeEach(context);

        if (metadata.migration().apply() == Migration.Mode.PER_METHOD) {
            var connectionCurrent = getContainerContext(context).connection();
            tryMigrateIfRequired(metadata, connectionCurrent);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var metadata = getMetadata(context);
        var storage = getStorage(context);
        storage.remove(Migration.class);
        if (metadata.migration().drop() == Migration.Mode.PER_METHOD) {
            if (metadata.runMode() != ContainerMode.PER_METHOD) {
                var connectionCurrent = getContainerContext(context).connection();
                tryDropIfRequired(metadata, connectionCurrent);
            }
        }

        super.afterEach(context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        var metadata = getMetadata(context);
        var connectionCurrent = getContainerContext(context).connection();
        if (metadata.migration().drop() == Migration.Mode.PER_CLASS) {
            if (metadata.runMode() == ContainerMode.PER_RUN) {
                tryDropIfRequired(metadata, connectionCurrent);
            }
        }

        super.afterAll(context);
    }
}
