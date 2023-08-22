package io.goodforgod.testcontainers.extensions.cassandra;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.*;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@Internal
class TestcontainersCassandraExtension extends
        AbstractTestcontainersExtension<CassandraConnection, CassandraContainer<?>, CassandraMetadata> {

    private static final String EXTERNAL_TEST_CASSANDRA_USERNAME = "EXTERNAL_TEST_CASSANDRA_USERNAME";
    private static final String EXTERNAL_TEST_CASSANDRA_PASSWORD = "EXTERNAL_TEST_CASSANDRA_PASSWORD";
    private static final String EXTERNAL_TEST_CASSANDRA_HOST = "EXTERNAL_TEST_CASSANDRA_HOST";
    private static final String EXTERNAL_TEST_CASSANDRA_PORT = "EXTERNAL_TEST_CASSANDRA_PORT";
    private static final String EXTERNAL_TEST_CASSANDRA_DATACENTER = "EXTERNAL_TEST_CASSANDRA_DATACENTER";

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersCassandraExtension.class);

    @Override
    protected Class<CassandraConnection> getConnectionType() {
        return CassandraConnection.class;
    }

    @Override
    protected CassandraContainer<?> getContainerDefault(CassandraMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("cassandra"));

        var alias = "cassandra-" + System.currentTimeMillis();
        var container = new CassandraContainer<>(dockerImage)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(CassandraContainer.class))
                        .withMdc("image", metadata.image())
                        .withMdc("alias", alias))
                .withNetworkAliases(alias)
                .withStartupTimeout(Duration.ofMinutes(5));

        if (metadata.useNetworkShared()) {
            container.withNetwork(Network.SHARED);
        }

        return container;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @SuppressWarnings("unchecked")
    protected Class<CassandraContainer<?>> getContainerType() {
        return (Class<CassandraContainer<?>>) ((Class<?>) CassandraContainer.class);
    }

    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerCassandra.class;
    }

    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ContainerCassandraConnection.class;
    }

    @NotNull
    protected Optional<CassandraMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersCassandra.class, context)
                .map(a -> new CassandraMetadata(a.network(), a.image(), a.mode(), a.migration()));
    }

    @NotNull
    protected CassandraConnection getConnectionForContainer(@NotNull CassandraContainer<?> container) {
        final String alias = container.getNetworkAliases().stream()
                .filter(a -> a.startsWith("cassandra"))
                .findFirst()
                .or(() -> (container.getNetworkAliases().isEmpty())
                        ? Optional.empty()
                        : Optional.of(container.getNetworkAliases().get(container.getNetworkAliases().size() - 1)))
                .orElse(null);

        return CassandraConnectionImpl.forContainer(container.getHost(),
                container.getMappedPort(CassandraContainer.CQL_PORT),
                alias,
                CassandraContainer.CQL_PORT,
                container.getLocalDatacenter(),
                container.getUsername(),
                container.getPassword());
    }

    @NotNull
    protected Optional<CassandraConnection> getConnectionExternal() {
        var host = System.getenv(EXTERNAL_TEST_CASSANDRA_HOST);
        var port = System.getenv(EXTERNAL_TEST_CASSANDRA_PORT);
        var user = System.getenv(EXTERNAL_TEST_CASSANDRA_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_CASSANDRA_PASSWORD);
        var dc = Optional.ofNullable(System.getenv(EXTERNAL_TEST_CASSANDRA_DATACENTER)).orElse("datacenter1");

        if (host != null && port != null) {
            return Optional.of(CassandraConnectionImpl.forExternal(host, Integer.parseInt(port), dc, user, password));
        } else
            return Optional.empty();
    }

    private static List<File> getFilesFromLocations(List<String> locations) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return locations.stream()
                .flatMap(location -> {
                    final URL url = loader.getResource(location);
                    final String path = url.getPath();
                    final File file = new File(path);
                    return file.isFile()
                            ? Stream.of(file)
                            : Arrays.stream(file.listFiles()).sorted();
                })
                .collect(Collectors.toList());
    }

    private static void migrateScripts(CassandraConnection connection, List<String> locations) {
        final Set<String> validLocations = locations.stream()
                .filter(Objects::nonNull)
                .filter(location -> !location.isBlank())
                .collect(Collectors.toSet());

        if (validLocations.isEmpty()) {
            throw new IllegalArgumentException("Found 0 valid migration paths: " + locations);
        }

        final List<File> filesToUseForMigration = getFilesFromLocations(locations);
        for (File file : filesToUseForMigration) {
            try {
                final String cql = Files.readString(file.toPath());
                final List<String> queries = Arrays.stream(cql.split(";"))
                        .map(query -> query + ";")
                        .collect(Collectors.toList());

                for (String query : queries) {
                    connection.execute(query);
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Illegal file for migration: " + file.getPath(), e);
            }
        }
    }

    private static class Table {

        private final String keyspace;
        private final String name;

        private Table(String keyspace, String name) {
            this.keyspace = keyspace;
            this.name = name;
        }

        public String keyspace() {
            return keyspace;
        }

        public String name() {
            return name;
        }
    }

    private static void dropScripts(CassandraConnection connection, List<String> locations) {
        var tables = ((CassandraConnectionImpl) connection).queryMany(
                "SELECT keyspace_name, table_name FROM system_schema.tables;",
                r -> new Table(r.getString(0), r.getString(1)));

        for (Table table : tables) {
            if (!table.keyspace().startsWith("system")) {
                connection.execute("TRUNCATE TABLE " + table.keyspace() + "." + table.name());
            }
        }
    }

    private void tryMigrateIfRequired(CassandraMetadata annotation, CassandraConnection cassandraConnection) {
        if (annotation.migration().engine() == Migration.Engines.SCRIPTS) {
            logger.debug("Starting schema migration for engine '{}' for connection: {}",
                    annotation.migration().engine(), cassandraConnection);
            migrateScripts(cassandraConnection, Arrays.asList(annotation.migration().migrations()));
            logger.debug("Finished schema migration for engine '{}' for connection: {}",
                    annotation.migration().engine(), cassandraConnection);
        }
    }

    private void tryDropIfRequired(CassandraMetadata annotation, CassandraConnection cassandraConnection) {
        if (annotation.migration().engine() == Migration.Engines.SCRIPTS) {
            logger.debug("Starting schema dropping for engine '{}' for connection: {}", annotation.migration().engine(),
                    cassandraConnection);
            dropScripts(cassandraConnection, Arrays.asList(annotation.migration().migrations()));
            logger.debug("Finished schema dropping for engine '{}' for connection: {}",
                    annotation.migration().engine(), cassandraConnection);
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        super.beforeAll(context);

        var metadata = getMetadata(context);
        var connectionCurrent = getConnectionCurrent(context);
        if (metadata.migration().apply() == Migration.Mode.PER_CLASS) {
            tryMigrateIfRequired(metadata, connectionCurrent);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        super.beforeEach(context);

        var metadata = getMetadata(context);
        var connectionCurrent = getConnectionCurrent(context);
        if (metadata.migration().apply() == Migration.Mode.PER_METHOD) {
            tryMigrateIfRequired(metadata, connectionCurrent);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var metadata = getMetadata(context);
        var connectionCurrent = getConnectionCurrent(context);
        if (metadata.migration().drop() == Migration.Mode.PER_METHOD) {
            tryDropIfRequired(metadata, connectionCurrent);
        }

        super.afterEach(context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        var metadata = getMetadata(context);
        var connectionCurrent = getConnectionCurrent(context);
        if (metadata.migration().drop() == Migration.Mode.PER_CLASS) {
            tryDropIfRequired(metadata, connectionCurrent);
        }

        super.afterAll(context);
    }
}
