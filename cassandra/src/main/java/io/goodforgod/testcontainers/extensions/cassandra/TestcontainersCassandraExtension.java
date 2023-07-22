package io.goodforgod.testcontainers.extensions.cassandra;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@Internal
class TestcontainersCassandraExtension implements
        BeforeAllCallback,
        BeforeEachCallback,
        AfterAllCallback,
        AfterEachCallback,
        ParameterResolver {

    private static final String EXTERNAL_TEST_CASSANDRA_USERNAME = "EXTERNAL_TEST_CASSANDRA_USERNAME";
    private static final String EXTERNAL_TEST_CASSANDRA_PASSWORD = "EXTERNAL_TEST_CASSANDRA_PASSWORD";
    private static final String EXTERNAL_TEST_CASSANDRA_HOST = "EXTERNAL_TEST_CASSANDRA_HOST";
    private static final String EXTERNAL_TEST_CASSANDRA_PORT = "EXTERNAL_TEST_CASSANDRA_PORT";
    private static final String EXTERNAL_TEST_CASSANDRA_DATACENTER = "EXTERNAL_TEST_CASSANDRA_DATACENTER";
    private static final String EXTERNAL_TEST_CASSANDRA_KEYSPACE = "EXTERNAL_TEST_CASSANDRA_KEYSPACE";

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersCassandraExtension.class);

    private static final Map<String, ExtensionContainerImpl> IMAGE_TO_SHARED_CONTAINER = new ConcurrentHashMap<>();

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private CassandraConnection externalConnection = null;

    private static final class ExtensionContainerImpl implements ExtensionContainer {

        private final CassandraContainer<?> container;
        private final CassandraConnection connection;

        ExtensionContainerImpl(CassandraContainer<?> container, CassandraConnection connection) {
            this.container = container;
            this.connection = connection;
        }

        CassandraConnection connection() {
            return connection;
        }

        @Override
        public void stop() {
            container.stop();
        }
    }

    static List<ExtensionContainer> getSharedContainers() {
        return new ArrayList<>(IMAGE_TO_SHARED_CONTAINER.values());
    }

    protected final <T extends Annotation> Optional<T> findAnnotation(Class<T> annotationType, ExtensionContext context) {
        Optional<ExtensionContext> current = Optional.of(context);
        while (current.isPresent()) {
            final Optional<T> annotation = AnnotationSupport.findAnnotation(current.get().getRequiredTestClass(), annotationType);
            if (annotation.isPresent()) {
                return annotation;
            }

            current = current.get().getParent();
        }

        return Optional.empty();
    }

    protected Optional<CassandraContainer<?>> getContainerFromField(ExtensionContext context) {
        logger.debug("Looking for Cassandra Container...");
        final Class<? extends Annotation> containerAnnotation = getContainerAnnotation();
        return ReflectionUtils.findFields(context.getRequiredTestClass(),
                f -> !f.isSynthetic() && f.getAnnotation(containerAnnotation) != null,
                ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .stream()
                .findFirst()
                .flatMap(field -> context.getTestInstance()
                        .map(instance -> {
                            try {
                                field.setAccessible(true);
                                Object possibleContainer = field.get(instance);
                                var containerType = getContainerType();
                                if (containerType.isAssignableFrom(possibleContainer.getClass())) {
                                    logger.debug("Found Cassandra Container in field: {}", field.getName());
                                    return ((CassandraContainer<?>) possibleContainer);
                                } else {
                                    throw new IllegalArgumentException(String.format(
                                            "Field '%s' annotated with @%s value must be instance of %s",
                                            field.getName(), containerAnnotation.getSimpleName(), containerType));
                                }
                            } catch (IllegalAccessException e) {
                                throw new IllegalStateException(
                                        String.format("Failed retrieving value from field '%s' annotated with @%s",
                                                field.getName(), containerAnnotation.getSimpleName()),
                                        e);
                            }
                        }));
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
    protected CassandraContainer<?> getDefaultContainer(@NotNull ContainerMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("cassandra"));

        var alias = "cassandra-" + System.currentTimeMillis();
        return new CassandraContainer<>(dockerImage)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(CassandraContainer.class))
                        .withMdc("image", metadata.image())
                        .withMdc("alias", alias))
                .withNetworkAliases(alias)
                .withNetwork(Network.SHARED)
                .withStartupTimeout(Duration.ofMinutes(5));
    }

    @NotNull
    protected Optional<ContainerMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersCassandra.class, context)
                .map(a -> new ContainerMetadata(a.image(), a.mode(), a.migration()));
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

        var connection = ((CassandraConnectionImpl) CassandraConnectionImpl.forContainer(container.getHost(),
                container.getMappedPort(CassandraContainer.CQL_PORT),
                alias,
                CassandraContainer.CQL_PORT,
                container.getLocalDatacenter(),
                null,
                container.getUsername(),
                container.getPassword()));

        var keyspace = "cassandra";
        connection.execute("CREATE KEYSPACE IF NOT EXISTS " + keyspace
                + " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};", null);

        return connection.withKeyspace(keyspace);
    }

    @NotNull
    protected Optional<CassandraConnection> getConnectionExternal() {
        var host = System.getenv(EXTERNAL_TEST_CASSANDRA_HOST);
        var port = System.getenv(EXTERNAL_TEST_CASSANDRA_PORT);
        var user = System.getenv(EXTERNAL_TEST_CASSANDRA_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_CASSANDRA_PASSWORD);
        var dc = Optional.ofNullable(System.getenv(EXTERNAL_TEST_CASSANDRA_DATACENTER)).orElse("datacenter1");
        var keyspace = System.getenv(EXTERNAL_TEST_CASSANDRA_KEYSPACE);

        if (host != null && port != null) {
            return Optional.of(CassandraConnectionImpl.forExternal(host, Integer.parseInt(port), dc, keyspace, user, password));
        } else
            return Optional.empty();
    }

    @Nullable
    private CassandraConnection getConnectionExternalCached() {
        if (this.externalConnection == null) {
            this.externalConnection = getConnectionExternal().orElse(null);
        }

        if (this.externalConnection != null) {
            logger.debug("Found external connection to database, no containers will be created during tests: {}",
                    externalConnection);
        }

        return this.externalConnection;
    }

    private ContainerMetadata getMetadata(@NotNull ExtensionContext context) {
        return findMetadata(context).orElseThrow(() -> new ExtensionConfigurationException("Extension annotation not found"));
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
                            : Arrays.stream(file.listFiles());
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
                ((CassandraConnectionImpl) connection).execute("TRUNCATE TABLE " + table.keyspace() + "." + table.name(), null);
            }
        }
    }

    private void tryMigrateIfRequired(ContainerMetadata annotation, CassandraConnection cassandraConnection) {
        if (annotation.migration().engine() == Migration.Engines.SCRIPTS) {
            logger.debug("Starting schema migration for engine '{}' for connection: {}",
                    annotation.migration().engine(), cassandraConnection);
            migrateScripts(cassandraConnection, Arrays.asList(annotation.migration().migrations()));
            logger.debug("Finished schema migration for engine '{}' for connection: {}",
                    annotation.migration().engine(), cassandraConnection);
        }
    }

    private void tryDropIfRequired(ContainerMetadata annotation, CassandraConnection cassandraConnection) {
        if (annotation.migration().engine() == Migration.Engines.SCRIPTS) {
            logger.debug("Starting schema dropping for engine '{}' for connection: {}", annotation.migration().engine(),
                    cassandraConnection);
            dropScripts(cassandraConnection, Arrays.asList(annotation.migration().migrations()));
            logger.debug("Finished schema dropping for engine '{}' for connection: {}",
                    annotation.migration().engine(), cassandraConnection);
        }
    }

    private void injectSqlConnection(CassandraConnection connection, ExtensionContext context) {
        var connectionAnnotation = getConnectionAnnotation();
        var connectionFields = ReflectionUtils.findFields(context.getRequiredTestClass(),
                f -> !f.isSynthetic()
                        && !Modifier.isFinal(f.getModifiers())
                        && !Modifier.isStatic(f.getModifiers())
                        && f.getAnnotation(connectionAnnotation) != null,
                ReflectionUtils.HierarchyTraversalMode.TOP_DOWN);

        logger.debug("Starting field injection for connection: {}", connection);
        context.getTestInstance().ifPresent(instance -> {
            for (Field field : connectionFields) {
                try {
                    field.setAccessible(true);
                    field.set(instance, connection);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(String.format("Field '%s' annotated with @%s can't set connection",
                            field.getName(), connectionAnnotation.getSimpleName()), e);
                }
            }
        });
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        var metadata = getMetadata(context);

        var externalConnection = getConnectionExternalCached();
        if (externalConnection != null) {
            if (metadata.migration().apply() == Migration.Mode.PER_CLASS) {
                tryMigrateIfRequired(metadata, externalConnection);
            }

            injectSqlConnection(externalConnection, context);
            return;
        }

        var storage = context.getStore(NAMESPACE);
        if (metadata.runMode() == ContainerMode.PER_RUN) {
            var containerFromField = getContainerFromField(context);
            var imageToLook = containerFromField.map(GenericContainer::getDockerImageName).orElseGet(metadata::image);

            var extensionContainer = IMAGE_TO_SHARED_CONTAINER.computeIfAbsent(imageToLook, k -> {
                var container = containerFromField.orElseGet(() -> {
                    logger.debug("Getting default Cassandra Container for image: {}", metadata.image());
                    return getDefaultContainer(metadata);
                });

                logger.debug("Starting in mode '{}' Cassandra Container: {}", metadata.runMode(), container);
                container.withReuse(true).start();
                logger.debug("Started successfully in mode '{}' Cassandra Container: {}", metadata.runMode(), container);
                var cqlConnection = getConnectionForContainer(container);
                return new ExtensionContainerImpl(container, cqlConnection);
            });

            storage.put(CassandraConnection.class, extensionContainer.connection());

            if (metadata.migration().apply() == Migration.Mode.PER_CLASS) {
                tryMigrateIfRequired(metadata, extensionContainer.connection());
            }
            injectSqlConnection(extensionContainer.connection(), context);
        } else if (metadata.runMode() == ContainerMode.PER_CLASS) {
            var container = getContainerFromField(context).orElseGet(() -> {
                logger.debug("Getting default Cassandra Container for image: {}", metadata.image());
                return getDefaultContainer(metadata);
            });

            logger.debug("Starting in mode '{}' Cassandra Container: {}", metadata.runMode(), container);
            container.start();
            logger.debug("Started successfully in mode '{}' Cassandra Container: {}", metadata.runMode(), container);
            var cqlConnection = getConnectionForContainer(container);
            var extensionContainer = new ExtensionContainerImpl(container, cqlConnection);
            storage.put(ContainerMode.PER_CLASS, extensionContainer);
            storage.put(CassandraConnection.class, cqlConnection);

            if (metadata.migration().apply() == Migration.Mode.PER_CLASS) {
                tryMigrateIfRequired(metadata, cqlConnection);
            }
            injectSqlConnection(cqlConnection, context);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        var metadata = getMetadata(context);

        var externalConnection = getConnectionExternalCached();
        if (externalConnection != null) {
            if (metadata.migration().apply() == Migration.Mode.PER_METHOD) {
                tryMigrateIfRequired(metadata, externalConnection);
            }
            injectSqlConnection(externalConnection, context);
            return;
        }

        var storage = context.getStore(NAMESPACE);
        if (metadata.runMode() == ContainerMode.PER_METHOD) {
            var container = getContainerFromField(context).orElseGet(() -> {
                logger.debug("Getting default Cassandra Container for image: {}", metadata.image());
                return getDefaultContainer(metadata);
            });

            logger.debug("Starting in mode '{}' Cassandra Container: {}", metadata.runMode(), container);
            container.start();
            logger.debug("Started successfully in mode '{}' Cassandra Container: {}", metadata.runMode(), container);
            var cqlConnection = getConnectionForContainer(container);

            if (metadata.migration().apply() == Migration.Mode.PER_METHOD) {
                tryMigrateIfRequired(metadata, cqlConnection);
            }

            injectSqlConnection(cqlConnection, context);
            storage.put(CassandraConnection.class, cqlConnection);
            storage.put(ContainerMode.PER_METHOD, new ExtensionContainerImpl(container, cqlConnection));
        } else {
            var cqlConnection = storage.get(CassandraConnection.class, CassandraConnection.class);
            if (metadata.migration().apply() == Migration.Mode.PER_METHOD) {
                tryMigrateIfRequired(metadata, cqlConnection);
            }
            injectSqlConnection(cqlConnection, context);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        var metadata = getMetadata(context);

        var externalConnection = getConnectionExternalCached();
        if (externalConnection != null) {
            if (metadata.migration().drop() == Migration.Mode.PER_METHOD) {
                tryDropIfRequired(metadata, externalConnection);
            }

            return;
        }

        var storage = context.getStore(NAMESPACE);
        if (metadata.runMode() == ContainerMode.PER_METHOD) {
            var extensionContainer = storage.get(ContainerMode.PER_METHOD, ExtensionContainerImpl.class);
            if (extensionContainer != null) {
                logger.debug("Stopping in mode '{}' Cassandra Container: {}", metadata.runMode(), extensionContainer.container);
                var connection = storage.get(CassandraConnection.class, CassandraConnection.class);
                ((CassandraConnectionImpl) connection).close();
                extensionContainer.stop();
                logger.debug("Stopped successfully in mode '{}' Cassandra Container: {}", metadata.runMode(),
                        extensionContainer.container);
            }
        } else if (metadata.runMode() == ContainerMode.PER_CLASS) {
            var extensionContainer = storage.get(ContainerMode.PER_CLASS, ExtensionContainerImpl.class);
            if (metadata.migration().drop() == Migration.Mode.PER_METHOD) {
                tryDropIfRequired(metadata, extensionContainer.connection());
            }
        } else if (metadata.runMode() == ContainerMode.PER_RUN) {
            Optional.ofNullable(IMAGE_TO_SHARED_CONTAINER.get(metadata.image())).ifPresent(extensionContainer -> {
                if (metadata.migration().drop() == Migration.Mode.PER_METHOD) {
                    tryDropIfRequired(metadata, extensionContainer.connection());
                }
            });
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        var metadata = getMetadata(context);

        var externalConnection = getConnectionExternalCached();
        if (externalConnection != null) {
            if (metadata.migration().drop() == Migration.Mode.PER_CLASS) {
                tryDropIfRequired(metadata, externalConnection);
            }

            return;
        }

        var storage = context.getStore(NAMESPACE);
        if (metadata.runMode() == ContainerMode.PER_CLASS) {
            var extensionContainer = storage.get(ContainerMode.PER_CLASS, ExtensionContainerImpl.class);
            if (extensionContainer != null) {
                logger.debug("Stopping in mode '{}' Cassandra Container: {}", metadata.runMode(), extensionContainer.container);
                var connection = storage.get(CassandraConnection.class, CassandraConnection.class);
                ((CassandraConnectionImpl) connection).close();
                extensionContainer.stop();
                logger.debug("Stopped successfully in mode '{}' Cassandra Container: {}", metadata.runMode(),
                        extensionContainer.container);
            }
        } else if (metadata.runMode() == ContainerMode.PER_RUN) {
            Optional.ofNullable(IMAGE_TO_SHARED_CONTAINER.get(metadata.image())).ifPresent(extensionContainer -> {
                if (metadata.migration().drop() == Migration.Mode.PER_CLASS) {
                    tryDropIfRequired(metadata, extensionContainer.connection());
                }
            });
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final Class<? extends Annotation> connectionAnnotation = getConnectionAnnotation();
        final boolean foundSuitable = parameterContext.getDeclaringExecutable() instanceof Method
                && parameterContext.getParameter().getAnnotation(connectionAnnotation) != null;

        if (!foundSuitable) {
            return false;
        }

        if (!parameterContext.getParameter().getType().equals(CassandraConnection.class)) {
            throw new ExtensionConfigurationException(String.format("Parameter '%s' annotated @%s is not of type %s",
                    parameterContext.getParameter().getName(), connectionAnnotation.getSimpleName(),
                    CassandraConnection.class));
        }

        return true;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        var externalConnection = getConnectionExternalCached();
        if (externalConnection != null) {
            return externalConnection;
        }

        var storage = extensionContext.getStore(NAMESPACE);
        return storage.get(CassandraConnection.class, CassandraConnection.class);
    }
}
