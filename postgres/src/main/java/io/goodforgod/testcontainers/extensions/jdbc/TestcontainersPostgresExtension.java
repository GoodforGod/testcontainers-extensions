package io.goodforgod.testcontainers.extensions.jdbc;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

final class TestcontainersPostgresExtension extends
        AbstractTestcontainersJdbcExtension<PostgreSQLContainerExtra<?>, PostgresMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersPostgresExtension.class);

    private static final String PROTOCOL = "postgresql";

    private static final String EXTERNAL_TEST_POSTGRES_JDBC_URL = "EXTERNAL_TEST_POSTGRES_JDBC_URL";
    private static final String EXTERNAL_TEST_POSTGRES_USERNAME = "EXTERNAL_TEST_POSTGRES_USERNAME";
    private static final String EXTERNAL_TEST_POSTGRES_PASSWORD = "EXTERNAL_TEST_POSTGRES_PASSWORD";
    private static final String EXTERNAL_TEST_POSTGRES_HOST = "EXTERNAL_TEST_POSTGRES_HOST";
    private static final String EXTERNAL_TEST_POSTGRES_PORT = "EXTERNAL_TEST_POSTGRES_PORT";
    private static final String EXTERNAL_TEST_POSTGRES_DATABASE = "EXTERNAL_TEST_POSTGRES_DATABASE";

    @SuppressWarnings("unchecked")
    @Override
    protected Class<PostgreSQLContainerExtra<?>> getContainerType() {
        return (Class<PostgreSQLContainerExtra<?>>) ((Class<?>) PostgreSQLContainerExtra.class);
    }

    @Override
    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerPostgres.class;
    }

    @Override
    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ContainerPostgresConnection.class;
    }

    @Override
    protected PostgreSQLContainerExtra<?> getContainerDefault(PostgresMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse(PostgreSQLContainer.IMAGE));

        var container = new PostgreSQLContainerExtra<>(dockerImage);
        container.setNetworkAliases(new ArrayList<>(List.of(metadata.networkAliasOrDefault())));
        if (metadata.networkShared()) {
            container.withNetwork(Network.SHARED);
        }

        return container;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @NotNull
    protected Optional<PostgresMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersPostgres.class, context)
                .map(a -> new PostgresMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(), a.migration()));
    }

    @NotNull
    protected JdbcConnection getConnectionForContainer(PostgresMetadata metadata,
                                                       @NotNull PostgreSQLContainerExtra<?> container) {
        return container.connection();
    }

    @NotNull
    protected Optional<JdbcConnection> getConnectionExternal() {
        var url = System.getenv(EXTERNAL_TEST_POSTGRES_JDBC_URL);
        var host = System.getenv(EXTERNAL_TEST_POSTGRES_HOST);
        var port = System.getenv(EXTERNAL_TEST_POSTGRES_PORT);
        var user = System.getenv(EXTERNAL_TEST_POSTGRES_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_POSTGRES_PASSWORD);
        var db = Optional.ofNullable(System.getenv(EXTERNAL_TEST_POSTGRES_DATABASE)).orElse("postgres");

        if (url != null) {
            if (host != null && port != null) {
                return Optional.of(JdbcConnectionImpl.forJDBC(url, host, Integer.parseInt(port), null, null, db, user, password));
            } else {
                return Optional.of(JdbcConnectionImpl.forExternal(url, user, password));
            }
        } else if (host != null && port != null) {
            return Optional.of(JdbcConnectionImpl.forProtocol(PROTOCOL, host, Integer.parseInt(port), db, user, password));
        } else {
            return Optional.empty();
        }
    }
}
