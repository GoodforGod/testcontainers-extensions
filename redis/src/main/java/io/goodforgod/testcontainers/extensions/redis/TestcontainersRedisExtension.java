package io.goodforgod.testcontainers.extensions.redis;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@Internal
class TestcontainersRedisExtension implements
        BeforeAllCallback,
        BeforeEachCallback,
        AfterAllCallback,
        AfterEachCallback,
        ParameterResolver {

    private static final String EXTERNAL_TEST_REDIS_USERNAME = "EXTERNAL_TEST_REDIS_USERNAME";
    private static final String EXTERNAL_TEST_REDIS_PASSWORD = "EXTERNAL_TEST_REDIS_PASSWORD";
    private static final String EXTERNAL_TEST_REDIS_HOST = "EXTERNAL_TEST_REDIS_HOST";
    private static final String EXTERNAL_TEST_REDIS_PORT = "EXTERNAL_TEST_REDIS_PORT";
    private static final String EXTERNAL_TEST_REDIS_DATABASE = "EXTERNAL_TEST_REDIS_DATABASE";

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersRedisExtension.class);

    private static final Map<String, ExtensionContainerImpl> IMAGE_TO_SHARED_CONTAINER = new ConcurrentHashMap<>();

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private RedisConnection externalConnection = null;

    private static final class ExtensionContainerImpl implements ExtensionContainer {

        private final RedisContainer container;
        private final RedisConnection connection;

        ExtensionContainerImpl(RedisContainer container, RedisConnection connection) {
            this.container = container;
            this.connection = connection;
        }

        RedisConnection connection() {
            return connection;
        }

        @Override
        public void stop() {
            container.stop();
        }

        @Override
        public String toString() {
            return container.getDockerImageName();
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

    protected Optional<RedisContainer> getContainerFromField(ExtensionContext context) {
        logger.debug("Looking for Redis Container...");
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
                                    logger.debug("Found Redis Container in field: {}", field.getName());
                                    return ((RedisContainer) possibleContainer);
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

    protected Class<RedisContainer> getContainerType() {
        return RedisContainer.class;
    }

    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerRedis.class;
    }

    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ContainerRedisConnection.class;
    }

    @NotNull
    protected RedisContainer getDefaultContainer(@NotNull ContainerMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("redis"));

        var alias = "redis-" + System.currentTimeMillis();
        return new RedisContainer(dockerImage)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(RedisContainer.class))
                        .withMdc("image", metadata.image())
                        .withMdc("alias", alias))
                .withNetworkAliases(alias)
                .withNetwork(Network.SHARED)
                .withStartupTimeout(Duration.ofMinutes(5));
    }

    @NotNull
    protected Optional<ContainerMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersRedis.class, context)
                .map(a -> new ContainerMetadata(a.image(), a.mode()));
    }

    @NotNull
    protected RedisConnection getConnectionForContainer(@NotNull RedisContainer container) {
        final String alias = container.getNetworkAliases().stream()
                .filter(a -> a.startsWith("redis"))
                .findFirst()
                .or(() -> (container.getNetworkAliases().isEmpty())
                        ? Optional.empty()
                        : Optional.of(container.getNetworkAliases().get(container.getNetworkAliases().size() - 1)))
                .orElse(null);

        return RedisConnectionImpl.forContainer(container.getHost(),
                container.getMappedPort(RedisContainer.PORT),
                alias,
                RedisContainer.PORT,
                container.getDatabase(),
                container.getUser(),
                container.getPassword());
    }

    @NotNull
    protected Optional<RedisConnection> getConnectionExternal() {
        var host = System.getenv(EXTERNAL_TEST_REDIS_HOST);
        var port = System.getenv(EXTERNAL_TEST_REDIS_PORT);
        var user = System.getenv(EXTERNAL_TEST_REDIS_USERNAME);
        var password = System.getenv(EXTERNAL_TEST_REDIS_PASSWORD);
        var database = Optional.ofNullable(System.getenv(EXTERNAL_TEST_REDIS_DATABASE)).map(Integer::parseInt).orElse(0);

        if (host != null && port != null) {
            return Optional.of(RedisConnectionImpl.forExternal(host, Integer.parseInt(port), database, user, password));
        } else
            return Optional.empty();
    }

    @Nullable
    private RedisConnection getConnectionExternalCached() {
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

    private void injectConnection(RedisConnection connection, ExtensionContext context) {
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
    public void beforeAll(ExtensionContext context) {
        var metadata = getMetadata(context);

        var externalConnection = getConnectionExternalCached();
        if (externalConnection != null) {
            injectConnection(externalConnection, context);
            return;
        }

        var storage = context.getStore(NAMESPACE);
        if (metadata.runMode() == ContainerMode.PER_RUN) {
            var containerFromField = getContainerFromField(context);
            var imageToLook = containerFromField.map(GenericContainer::getDockerImageName).orElseGet(metadata::image);

            var extensionContainer = IMAGE_TO_SHARED_CONTAINER.computeIfAbsent(imageToLook, k -> {
                var container = containerFromField.orElseGet(() -> {
                    logger.debug("Getting default Redis Container for image: {}", metadata.image());
                    return getDefaultContainer(metadata);
                });

                logger.debug("Starting in mode '{}' Redis Container: {}", metadata.runMode(), container.getDockerImageName());
                container.withReuse(true).start();
                logger.debug("Started successfully in mode '{}' Redis Container: {}", metadata.runMode(),
                        container.getDockerImageName());
                var queryConnection = getConnectionForContainer(container);
                return new ExtensionContainerImpl(container, queryConnection);
            });

            storage.put(RedisConnection.class, extensionContainer.connection());

            injectConnection(extensionContainer.connection(), context);
        } else if (metadata.runMode() == ContainerMode.PER_CLASS) {
            var container = getContainerFromField(context).orElseGet(() -> {
                logger.debug("Getting default Redis Container for image: {}", metadata.image());
                return getDefaultContainer(metadata);
            });

            logger.debug("Starting in mode '{}' Redis Container: {}", metadata.runMode(), container.getDockerImageName());
            container.start();
            logger.debug("Started successfully in mode '{}' Redis Container: {}", metadata.runMode(),
                    container.getDockerImageName());
            var connection = getConnectionForContainer(container);
            var extensionContainer = new ExtensionContainerImpl(container, connection);
            storage.put(ContainerMode.PER_CLASS, extensionContainer);
            storage.put(RedisConnection.class, connection);

            injectConnection(connection, context);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var metadata = getMetadata(context);

        var externalConnection = getConnectionExternalCached();
        if (externalConnection != null) {
            injectConnection(externalConnection, context);
            return;
        }

        var storage = context.getStore(NAMESPACE);
        if (metadata.runMode() == ContainerMode.PER_METHOD) {
            var container = getContainerFromField(context).orElseGet(() -> {
                logger.debug("Getting default Redis Container for image: {}", metadata.image());
                return getDefaultContainer(metadata);
            });

            logger.debug("Starting in mode '{}' Redis Container: {}", metadata.runMode(), container.getDockerImageName());
            container.start();
            logger.debug("Started successfully in mode '{}' Redis Container: {}", metadata.runMode(),
                    container.getDockerImageName());
            var queryConnection = getConnectionForContainer(container);

            injectConnection(queryConnection, context);

            storage.put(RedisConnection.class, queryConnection);
            storage.put(ContainerMode.PER_METHOD, new ExtensionContainerImpl(container, queryConnection));
        } else {
            var queryConnection = storage.get(RedisConnection.class, RedisConnection.class);

            injectConnection(queryConnection, context);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var metadata = getMetadata(context);

        var externalConnection = getConnectionExternalCached();
        if (externalConnection != null) {
            return;
        }

        var storage = context.getStore(NAMESPACE);
        if (metadata.runMode() == ContainerMode.PER_METHOD) {
            var extensionContainer = storage.get(ContainerMode.PER_METHOD, ExtensionContainerImpl.class);
            if (extensionContainer != null) {
                logger.debug("Stopping in mode '{}' Redis Container: {}", metadata.runMode(),
                        extensionContainer.container.getDockerImageName());
                var connection = storage.get(RedisConnection.class, RedisConnection.class);
                ((RedisConnectionImpl) connection).close();
                extensionContainer.stop();
                logger.debug("Stopped successfully in mode '{}' Redis Container: {}", metadata.runMode(),
                        extensionContainer.container.getDockerImageName());
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        var metadata = getMetadata(context);

        var externalConnection = getConnectionExternalCached();
        if (externalConnection != null) {
            return;
        }

        var storage = context.getStore(NAMESPACE);
        if (metadata.runMode() == ContainerMode.PER_CLASS) {
            var extensionContainer = storage.get(ContainerMode.PER_CLASS, ExtensionContainerImpl.class);
            if (extensionContainer != null) {
                logger.debug("Stopping in mode '{}' Redis Container: {}", metadata.runMode(),
                        extensionContainer.container.getDockerImageName());
                var connection = storage.get(RedisConnection.class, RedisConnection.class);
                ((RedisConnectionImpl) connection).close();
                extensionContainer.stop();
                logger.debug("Stopped successfully in mode '{}' Redis Container: {}", metadata.runMode(),
                        extensionContainer.container.getDockerImageName());
            }
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

        if (!parameterContext.getParameter().getType().equals(RedisConnection.class)) {
            throw new ExtensionConfigurationException(String.format("Parameter '%s' annotated @%s is not of type %s",
                    parameterContext.getParameter().getName(), connectionAnnotation.getSimpleName(),
                    RedisConnection.class));
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
        return storage.get(RedisConnection.class, RedisConnection.class);
    }
}
