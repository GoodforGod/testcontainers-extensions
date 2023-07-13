package io.goodforgod.testcontainers.extensions.jdbc;

import java.io.FileWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.flywaydb.core.Flyway;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;

@Internal
abstract class AbstractTestcontainersJdbcExtension implements
        BeforeAllCallback,
        BeforeEachCallback,
        AfterAllCallback,
        AfterEachCallback,
        TestExecutionListener,
        ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(AbstractTestcontainersJdbcExtension.class);

    private static final Map<String, ExtensionContainer> IMAGE_TO_SHARED_CONTAINER = new ConcurrentHashMap<>();
    private static volatile JdbcConnection externalConnection = null;
    private static volatile boolean isLiquibaseActivated = false;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private record ExtensionContainer(JdbcDatabaseContainer<?> container, JdbcConnection connection) {}

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

    protected Optional<JdbcDatabaseContainer<?>> getContainerFromField(ExtensionContext context) {
        logger.debug("Looking for JDBC Container...");
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
                                    logger.debug("Found SQL Container in field: {}", field.getName());
                                    return ((JdbcDatabaseContainer<?>) possibleContainer);
                                } else {
                                    throw new IllegalArgumentException(
                                            "Field '%s' annotated with @%s value must be instance of %s".formatted(
                                                    field.getName(), containerAnnotation.getSimpleName(), containerType));
                                }
                            } catch (IllegalAccessException e) {
                                throw new IllegalStateException("Failed retrieving value from field '%s' annotated with @%s"
                                        .formatted(field.getName(), containerAnnotation.getSimpleName()), e);
                            }
                        }));
    }

    abstract Class<? extends JdbcDatabaseContainer> getContainerType();

    abstract Class<? extends Annotation> getContainerAnnotation();

    abstract Class<? extends Annotation> getConnectionAnnotation();

    @NotNull
    abstract JdbcDatabaseContainer<?> getDefaultContainer(@NotNull String image);

    @NotNull
    abstract Optional<ContainerMetadata> findMetadata(@NotNull ExtensionContext context);

    @NotNull
    abstract JdbcConnection getConnectionForContainer(@NotNull JdbcDatabaseContainer<?> container);

    @NotNull
    abstract Optional<JdbcConnection> getConnectionExternal();

    private ContainerMetadata getMetadata(@NotNull ExtensionContext context) {
        return findMetadata(context).orElseThrow(() -> new ExtensionConfigurationException("Extension annotation not found"));
    }

    private static Flyway getFlyway(JdbcConnection connection, List<String> locations) {
        final List<String> migrationLocations = (locations.isEmpty())
                ? List.of("classpath:db/migration")
                : locations;

        return Flyway.configure()
                .loggers("slf4j")
                .dataSource(connection.params().jdbcUrl(), connection.params().username(), connection.params().password())
                .locations(migrationLocations.toArray(String[]::new))
                .cleanDisabled(false)
                .load();
    }

    private static void migrateFlyway(JdbcConnection connection, List<String> locations) {
        getFlyway(connection, locations).migrate();
    }

    private static void dropFlyway(JdbcConnection connection, List<String> locations) {
        getFlyway(connection, locations).clean();
    }

    @FunctionalInterface
    interface LiquibaseRunner {

        void apply(Liquibase liquibase, Writer writer) throws LiquibaseException;
    }

    private static void prepareLiquibase(JdbcConnection connection, List<String> locations, LiquibaseRunner liquibaseConsumer) {
        try {
            final List<String> changeLogLocations = (locations.isEmpty())
                    ? List.of("db/changelog.sql")
                    : locations;

            if (!isLiquibaseActivated) {
                final boolean julEnabled = Optional.ofNullable(System.getenv("TEST_CONTAINERS_EXTENSION_SQL_JUL_ENABLED"))
                        .map(Boolean::parseBoolean)
                        .orElse(true);

                if (julEnabled) {
                    SLF4JBridgeHandler.removeHandlersForRootLogger();
                    SLF4JBridgeHandler.install();
                    isLiquibaseActivated = true;
                }
            }

            try (var con = connection.open()) {
                var liquibaseConnection = new liquibase.database.jvm.JdbcConnection(con);
                var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(liquibaseConnection);
                for (String changeLog : changeLogLocations) {
                    try (var classLoaderResourceAccessor = new ClassLoaderResourceAccessor()) {
                        try (var liquibase = new Liquibase(changeLog, classLoaderResourceAccessor, database)) {
                            var tmpFile = Files.createTempFile("liquibase-changelog-output", ".txt");
                            try (var writer = new FileWriter(tmpFile.toFile())) {
                                liquibaseConsumer.apply(liquibase, writer);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void migrateLiquibase(JdbcConnection connection, List<String> locations) {
        prepareLiquibase(connection, locations, (liquibase, writer) -> {
            var contexts = new Contexts();
            var labelExpression = new LabelExpression();
            var changeSetStatuses = liquibase.getChangeSetStatuses(contexts, labelExpression, true);
            if (!changeSetStatuses.isEmpty()) {
                liquibase.update();
            }
        });
    }

    private static void dropLiquibase(JdbcConnection connection, List<String> locations) {
        prepareLiquibase(connection, locations, (liquibase, writer) -> liquibase.dropAll());
    }

    private void tryMigrateIfRequired(ContainerMetadata annotation, JdbcConnection jdbcConnection) {
        if (annotation.migration().engine() == Migration.Engines.FLYWAY) {
            logger.debug("Starting schema migration for engine '{}' for connection: {}", annotation.migration().engine(),
                    jdbcConnection);
            migrateFlyway(jdbcConnection, Arrays.asList(annotation.migration().migrations()));
        } else if (annotation.migration().engine() == Migration.Engines.LIQUIBASE) {
            logger.debug("Starting schema migration for engine '{}' for connection: {}", annotation.migration().engine(),
                    jdbcConnection);
            migrateLiquibase(jdbcConnection, Arrays.asList(annotation.migration().migrations()));
        }
    }

    private void tryDropIfRequired(ContainerMetadata annotation, JdbcConnection jdbcConnection) {
        if (annotation.migration().engine() == Migration.Engines.FLYWAY) {
            logger.debug("Starting schema dropping for engine '{}' for connection: {}", annotation.migration().engine(),
                    jdbcConnection);
            dropFlyway(jdbcConnection, Arrays.asList(annotation.migration().migrations()));
        } else if (annotation.migration().engine() == Migration.Engines.LIQUIBASE) {
            logger.debug("Starting schema dropping for engine '{}' for connection: {}", annotation.migration().engine(),
                    jdbcConnection);
            dropLiquibase(jdbcConnection, Arrays.asList(annotation.migration().migrations()));
        }
    }

    private void injectSqlConnection(JdbcConnection connection, ExtensionContext context) {
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
                    throw new IllegalStateException("Field '%s' annotated with @%s can't set connection".formatted(
                            field.getName(), connectionAnnotation.getSimpleName()), e);
                }
            }
        });
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        externalConnection = getConnectionExternal().orElse(null);
        if (externalConnection != null) {
            logger.debug("Found external connection to database, no containers will be created during tests: {}",
                    externalConnection);
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        var metadata = getMetadata(context);

        if (externalConnection != null) {
            if (metadata.migration().apply() == Migration.Mode.PER_CLASS) {
                tryMigrateIfRequired(metadata, externalConnection);
            }

            return;
        }

        var storage = context.getStore(NAMESPACE);
        if (metadata.runMode() == ContainerMode.PER_RUN) {
            var containerFromField = getContainerFromField(context);
            var imageToLook = containerFromField.map(GenericContainer::getDockerImageName).orElseGet(metadata::image);

            var extensionContainer = IMAGE_TO_SHARED_CONTAINER.computeIfAbsent(imageToLook, k -> {
                var container = containerFromField.orElseGet(() -> {
                    logger.debug("Getting default SQL Container for image: {}", metadata.image());
                    return getDefaultContainer(metadata.image());
                });

                logger.debug("Starting in mode '{}' SQL Container: {}", metadata.runMode(), container);
                container.start();
                container.withReuse(true);
                var sqlConnection = getConnectionForContainer(container);
                return new ExtensionContainer(container, sqlConnection);
            });

            storage.put(JdbcConnection.class, extensionContainer.connection());

            if (metadata.migration().apply() == Migration.Mode.PER_CLASS) {
                tryMigrateIfRequired(metadata, extensionContainer.connection());
            }
        } else if (metadata.runMode() == ContainerMode.PER_CLASS) {
            var container = getContainerFromField(context).orElseGet(() -> {
                logger.debug("Getting default SQL Container for image: {}", metadata.image());
                return getDefaultContainer(metadata.image());
            });

            logger.debug("Starting in mode '{}' SQL Container: {}", metadata.runMode(), container);
            container.start();
            var sqlConnection = getConnectionForContainer(container);
            var extensionContainer = new ExtensionContainer(container, sqlConnection);
            storage.put(ContainerMode.PER_CLASS, extensionContainer);
            storage.put(JdbcConnection.class, sqlConnection);

            if (metadata.migration().apply() == Migration.Mode.PER_CLASS) {
                tryMigrateIfRequired(metadata, sqlConnection);
            }
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        var metadata = getMetadata(context);

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
                logger.debug("Getting default SQL Container for image: {}", metadata.image());
                return getDefaultContainer(metadata.image());
            });

            logger.debug("Starting in mode '{}' SQL Container: {}", metadata.runMode(), container);
            container.start();
            var sqlConnection = getConnectionForContainer(container);

            if (metadata.migration().apply() == Migration.Mode.PER_METHOD) {
                tryMigrateIfRequired(metadata, sqlConnection);
            }

            injectSqlConnection(sqlConnection, context);
            storage.put(JdbcConnection.class, sqlConnection);
            storage.put(ContainerMode.PER_METHOD, new ExtensionContainer(container, sqlConnection));
        } else {
            var sqlConnection = storage.get(JdbcConnection.class, JdbcConnection.class);
            injectSqlConnection(sqlConnection, context);

            if (metadata.migration().apply() == Migration.Mode.PER_METHOD) {
                tryMigrateIfRequired(metadata, sqlConnection);
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        var metadata = getMetadata(context);

        if (externalConnection != null) {
            if (metadata.migration().drop() == Migration.Mode.PER_METHOD) {
                tryDropIfRequired(metadata, externalConnection);
            }

            return;
        }

        var storage = context.getStore(NAMESPACE);
        if (metadata.runMode() == ContainerMode.PER_METHOD) {
            var extensionContainer = storage.get(ContainerMode.PER_METHOD, ExtensionContainer.class);
            if (extensionContainer != null) {
                logger.debug("Stopping in mode '{}' SQL Container: {}", metadata.runMode(), extensionContainer.container);
                extensionContainer.container().stop();
            }
        } else if (metadata.runMode() == ContainerMode.PER_CLASS) {
            var extensionContainer = storage.get(ContainerMode.PER_CLASS, ExtensionContainer.class);
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

        if (externalConnection != null) {
            if (metadata.migration().drop() == Migration.Mode.PER_CLASS) {
                tryDropIfRequired(metadata, externalConnection);
            }

            return;
        }

        var storage = context.getStore(NAMESPACE);
        if (metadata.runMode() == ContainerMode.PER_CLASS) {
            var extensionContainer = storage.get(ContainerMode.PER_CLASS, ExtensionContainer.class);
            if (extensionContainer != null) {
                logger.debug("Stopping in mode '{}' SQL Container: {}", metadata.runMode(), extensionContainer.container);
                extensionContainer.container().stop();
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
    public void testPlanExecutionFinished(TestPlan testPlan) {
        for (ExtensionContainer container : IMAGE_TO_SHARED_CONTAINER.values()) {
            logger.debug("Stopping in mode '{}' SQL Container: {}", ContainerMode.PER_RUN, container);
            container.container().stop();
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

        if (!parameterContext.getParameter().getType().equals(JdbcConnection.class)) {
            throw new ExtensionConfigurationException("Parameter '%s' annotated @%s is not of type %s".formatted(
                    parameterContext.getParameter().getName(), connectionAnnotation.getSimpleName(),
                    JdbcConnection.class));
        }

        return true;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (externalConnection != null) {
            return externalConnection;
        }

        var storage = extensionContext.getStore(NAMESPACE);
        return storage.get(JdbcConnection.class, JdbcConnection.class);
    }
}
