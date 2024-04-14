package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

final class TestcontainersMariaDBExtension extends
        AbstractTestcontainersJdbcExtension<org.testcontainers.containers.MariaDBContainer<?>, JdbcMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersMariaDBExtension.class);

    @SuppressWarnings("unchecked")
    @Override
    protected Class<org.testcontainers.containers.MariaDBContainer<?>> getContainerType() {
        return (Class<org.testcontainers.containers.MariaDBContainer<?>>) ((Class<?>) org.testcontainers.containers.MariaDBContainer.class);
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerMariaDB.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ConnectionMariaDB.class;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @Override
    protected org.testcontainers.containers.MariaDBContainer<?> createContainerDefault(JdbcMetadata metadata) {
        var image = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse(org.testcontainers.containers.MariaDBContainer.NAME));

        final MariaDBContainer<?> container = new MariaDBContainer<>(image);
        final String alias = Optional.ofNullable(metadata.networkAlias())
                .orElseGet(() -> "mariadb-" + System.currentTimeMillis());
        container.withDatabaseName(MariaDBContext.DATABASE_NAME);
        container.withUsername("mariadb");
        container.withPassword("mariadb");
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(MariaDBContainer.class))
                .withMdc("image", image.asCanonicalNameString())
                .withMdc("alias", alias));
        container.waitingFor(Wait.forListeningPort());
        container.withStartupTimeout(Duration.ofMinutes(5));
        container.setNetworkAliases(new ArrayList<>(List.of(alias)));
        if (metadata.networkShared()) {
            container.withNetwork(Network.SHARED);
        }

        return container;
    }

    @Override
    protected ContainerContext<JdbcConnection>
            createContainerContext(org.testcontainers.containers.MariaDBContainer<?> container) {
        return new MariaDBContext(container);
    }

    @NotNull
    protected Optional<JdbcMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersMariaDB.class, context)
                .map(a -> new JdbcMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(), a.migration()));
    }
}
