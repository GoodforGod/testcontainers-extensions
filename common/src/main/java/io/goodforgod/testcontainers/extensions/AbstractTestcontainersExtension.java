package io.goodforgod.testcontainers.extensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

@Internal
public abstract class AbstractTestcontainersExtension<Connection, Container extends GenericContainer<?>, Metadata extends ContainerMetadata>
        implements
        BeforeAllCallback,
        BeforeEachCallback,
        AfterAllCallback,
        AfterEachCallback,
        ParameterResolver {

    public enum CallMode {
        CONSTRUCTOR,
        BEFORE_EACH,
        BEFORE_ALL,
    }

    interface SharedKey {}

    static final class SharedContainerInstance implements SharedKey {

        private final GenericContainer<?> container;

        public SharedContainerInstance(GenericContainer<?> container) {
            this.container = container;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            SharedContainerInstance sharedKey = (SharedContainerInstance) o;
            return container == sharedKey.container;
        }

        @Override
        public int hashCode() {
            return Objects.hash(container);
        }
    }

    static final class SharedContainerKey implements SharedKey {

        private final String image;
        private final boolean network;
        private final String alias;

        SharedContainerKey(String image, boolean network, String alias) {
            this.image = image;
            this.network = network;
            this.alias = alias;
        }

        public String image() {
            return image;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            SharedContainerKey sharedKey = (SharedContainerKey) o;
            return network == sharedKey.network && Objects.equals(image, sharedKey.image)
                    && Objects.equals(alias, sharedKey.alias);
        }

        @Override
        public int hashCode() {
            return Objects.hash(image, network, alias);
        }

        @Override
        public String toString() {
            return (alias == null)
                    ? "[image=" + image + ']'
                    : "[image=" + image + ", alias=" + alias + ']';
        }
    }

    static final Map<String, Map<SharedKey, ExtensionContainer<?, ?>>> CLASS_TO_SHARED_CONTAINERS = new ConcurrentHashMap<>();

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected abstract Class<? extends Annotation> getContainerAnnotation();

    protected abstract Class<? extends Annotation> getConnectionAnnotation();

    protected abstract Class<Connection> getConnectionType();

    protected abstract Class<Container> getContainerType();

    protected abstract Optional<Metadata> findMetadata(ExtensionContext context);

    protected abstract Container getContainerDefault(Metadata metadata);

    protected abstract Connection getConnectionForContainer(Metadata metadata, Container container);

    protected abstract ExtensionContext.Namespace getNamespace();

    protected ExtensionContainer<Container, Connection> getExtensionContainer(Container container, Connection connection) {
        return new ExtensionContainerImpl<>(container, connection);
    }

    protected final ExtensionContext.Store getStorage(ExtensionContext context) {
        if (context.getParent().isPresent() && context.getParent().get().getParent().isPresent()) {
            return context.getParent().get().getStore(getNamespace());
        } else if (context.getParent().isEmpty()) {
            return context.getStore(getNamespace());
        } else {
            return context.getStore(getNamespace());
        }
    }

    protected final Metadata getMetadata(ExtensionContext context) {
        return findMetadata(context)
                .orElseThrow(() -> new ExtensionConfigurationException("Extension annotation not found"));
    }

    protected final Connection getConnectionCurrent(ExtensionContext context) {
        return getStorage(context).get(getConnectionType(), getConnectionType());
    }

    protected final Container getContainerCurrent(ExtensionContext context) {
        Metadata metadata = getMetadata(context);
        ExtensionContainer<Container, Connection> extensionContainer = getStorage(context).get(metadata.runMode(),
                ExtensionContainer.class);
        return extensionContainer.container();
    }

    protected <T extends Annotation> Optional<T> findAnnotation(Class<T> annotationType, ExtensionContext context) {
        Optional<ExtensionContext> current = Optional.of(context);
        while (current.isPresent()) {
            Class<?> requiredClass = current.get().getRequiredTestClass();
            while (!requiredClass.equals(Object.class)) {
                final Optional<T> annotation = AnnotationSupport.findAnnotation(requiredClass, annotationType);
                if (annotation.isPresent()) {
                    return annotation;
                }

                requiredClass = requiredClass.getSuperclass();
            }

            current = current.get().getParent();
        }

        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    protected Optional<Container> getContainerFromField(ExtensionContext context) {
        logger.debug("Looking for {} Container...", getContainerType().getSimpleName());
        final Optional<Class<?>> testClass = context.getTestClass();
        if (testClass.isEmpty()) {
            return Optional.empty();
        }

        return ReflectionUtils.findFields(testClass.get(),
                f -> !f.isSynthetic() && f.getAnnotation(getContainerAnnotation()) != null,
                ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .stream()
                .findFirst()
                .flatMap(field -> context.getTestInstance()
                        .map(instance -> {
                            try {
                                field.setAccessible(true);
                                Object possibleContainer = field.get(instance);
                                if (getContainerType().isAssignableFrom(possibleContainer.getClass())) {
                                    logger.debug("Found {} Container in field: {}", getContainerType().getSimpleName(),
                                            field.getName());
                                    return ((Container) possibleContainer);
                                } else {
                                    throw new IllegalArgumentException(String.format(
                                            "Field '%s' annotated with @%s value must be instance of %s",
                                            field.getName(), getContainerAnnotation().getSimpleName(), getContainerType()));
                                }
                            } catch (IllegalAccessException e) {
                                throw new IllegalStateException(
                                        String.format("Failed retrieving value from field '%s' annotated with @%s",
                                                field.getName(), getContainerAnnotation().getSimpleName()),
                                        e);
                            }
                        }));
    }

    protected void injectConnection(Connection connection, ExtensionContext context) {
        Class<? extends Annotation> connectionAnnotation = getConnectionAnnotation();
        List<Field> connectionFields = ReflectionUtils.findFields(context.getRequiredTestClass(),
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
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final Class<? extends Annotation> connectionAnnotation = getConnectionAnnotation();
        final boolean foundSuitable = parameterContext.getParameter().getAnnotation(connectionAnnotation) != null;
        if (!foundSuitable) {
            return false;
        }

        if (!parameterContext.getParameter().getType().equals(getConnectionType())) {
            throw new ParameterResolutionException(String.format("Parameter '%s' annotated @%s is not of type %s",
                    parameterContext.getParameter().getName(), connectionAnnotation.getSimpleName(),
                    getConnectionType()));
        }

        return true;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        CallMode callMode = getCallMode(parameterContext);
        Connection connection = getConnectionCurrent(extensionContext);
        if (connection != null) {
            return connection;
        } else {
            Metadata metadata = getMetadata(extensionContext);
            if (metadata.runMode() == ContainerMode.PER_RUN || metadata.runMode() == ContainerMode.PER_CLASS) {
                beforeAll(extensionContext);
            } else if (metadata.runMode() == ContainerMode.PER_METHOD) {
                TestInstance.Lifecycle lifecycle = extensionContext.getTestInstanceLifecycle()
                        .orElse(TestInstance.Lifecycle.PER_METHOD);
                if (callMode == CallMode.CONSTRUCTOR && lifecycle == TestInstance.Lifecycle.PER_CLASS) {
                    throw new ParameterResolutionException(String.format(
                            "@%s can't be injected into constructor parameter when ContainerMode.%s is used and lifecycle is @%s",
                            getConnectionAnnotation().getSimpleName(), ContainerMode.PER_METHOD,
                            TestInstance.Lifecycle.PER_CLASS));
                } else if (callMode == CallMode.BEFORE_ALL) {
                    throw new ParameterResolutionException(
                            String.format("@%s can't be injected into @%s method parameter when ContainerMode.%s is used",
                                    getConnectionAnnotation().getSimpleName(), BeforeAll.class.getSimpleName(),
                                    ContainerMode.PER_METHOD));
                }

                beforeEach(extensionContext);
            }

            return Optional.ofNullable(getConnectionCurrent(extensionContext))
                    .orElseThrow(() -> new ParameterResolutionException(String.format(
                            "Parameter named '%s' with type '%s' can't be resolved cause it probably isn't initialized yet, please check extension annotation execution order",
                            parameterContext.getParameter().getName(), getConnectionType())));
        }
    }

    private CallMode getCallMode(ParameterContext parameterContext) {
        if (parameterContext.getDeclaringExecutable().isAnnotationPresent(BeforeAll.class)) {
            return CallMode.BEFORE_ALL;
        } else if (parameterContext.getDeclaringExecutable().isAnnotationPresent(BeforeEach.class)) {
            return CallMode.BEFORE_EACH;
        } else if (parameterContext.getDeclaringExecutable().getDeclaringClass().getName()
                .equals(parameterContext.getDeclaringExecutable().getName())) {
            return CallMode.CONSTRUCTOR;
        } else {
            return null;
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        final Metadata metadata = getMetadata(context);
        final ExtensionContext.Store storage = getStorage(context);
        final Connection storageConnection = getConnectionCurrent(context);
        if (storageConnection == null) {
            if (metadata.runMode() == ContainerMode.PER_RUN) {
                final Optional<Container> containerFromField = getContainerFromField(context);
                final SharedKey sharedKey = containerFromField
                        .map(c -> ((SharedKey) new SharedContainerInstance(c)))
                        .orElseGet(() -> {
                            final String imageShared = metadata.image();
                            final Boolean networkShared = containerFromField.filter(c -> c.getNetwork() != null)
                                    .map(c -> c.getNetwork() == Network.SHARED)
                                    .orElse(metadata.networkShared());

                            final String networkAlias = containerFromField.map(c -> c.getNetworkAliases())
                                    .filter(a -> !a.isEmpty())
                                    .map(a -> a.stream()
                                            .filter(alias -> alias.equals(metadata.networkAlias()))
                                            .findFirst()
                                            .orElse(a.get(0)))
                                    .orElse(metadata.networkAlias());

                            return new SharedContainerKey(imageShared, networkShared, networkAlias);
                        });

                var sharedContainerMap = CLASS_TO_SHARED_CONTAINERS.computeIfAbsent(
                        getClass().getCanonicalName(),
                        k -> new ConcurrentHashMap<>());

                var extensionContainer = sharedContainerMap.computeIfAbsent(sharedKey, k -> {
                    Container container = containerFromField.orElseGet(() -> {
                        logger.debug("Getting default container for image: {}", metadata.image());
                        return getContainerDefault(metadata);
                    });

                    logger.debug("Starting in mode '{}' container: {}", metadata.runMode(), container.getDockerImageName());
                    container.withReuse(true).start();
                    logger.info("Started in mode '{}' container: {}", metadata.runMode(),
                            container.getDockerImageName());
                    Connection connection = getConnectionForContainer(metadata, container);
                    return getExtensionContainer(container, connection);
                });

                storage.put(metadata.runMode(), extensionContainer);
                storage.put(getConnectionType(), extensionContainer.connection());
                injectConnection((Connection) extensionContainer.connection(), context);
            } else if (metadata.runMode() == ContainerMode.PER_CLASS) {
                Container container = getContainerFromField(context).orElseGet(() -> {
                    logger.debug("Getting default container for image: {}", metadata.image());
                    return getContainerDefault(metadata);
                });

                logger.debug("Starting in mode '{}' container: {}", metadata.runMode(), container.getDockerImageName());
                container.start();
                logger.info("Started in mode '{}' container: {}", metadata.runMode(), container.getDockerImageName());
                Connection connection = getConnectionForContainer(metadata, container);
                ExtensionContainer<Container, Connection> extensionContainer = getExtensionContainer(container, connection);
                storage.put(metadata.runMode(), extensionContainer);
                storage.put(getConnectionType(), connection);
                injectConnection(connection, context);
            }
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        Metadata metadata = getMetadata(context);
        ExtensionContext.Store storage = getStorage(context);
        Connection storageConnection = getConnectionCurrent(context);
        if (storageConnection == null) {
            if (metadata.runMode() == ContainerMode.PER_METHOD) {
                Container container = getContainerFromField(context).orElseGet(() -> {
                    logger.debug("Getting default container for image: {}", metadata.image());
                    return getContainerDefault(metadata);
                });

                logger.debug("Starting in mode '{}' container: {}", metadata.runMode(), container.getDockerImageName());
                container.start();
                logger.info("Started in mode '{}' container: {}", metadata.runMode(), container.getDockerImageName());
                Connection connection = getConnectionForContainer(metadata, container);
                ExtensionContainer<Container, Connection> extensionContainer = getExtensionContainer(container, connection);
                storage.put(metadata.runMode(), extensionContainer);
                storage.put(getConnectionType(), connection);
            }
        }

        TestInstance.Lifecycle lifecycle = context.getTestInstanceLifecycle().orElse(TestInstance.Lifecycle.PER_METHOD);
        if (lifecycle == TestInstance.Lifecycle.PER_METHOD) {
            Connection connection = getConnectionCurrent(context);
            if (connection != null) {
                injectConnection(connection, context);
            }
        } else if (metadata.runMode() == ContainerMode.PER_METHOD) {
            Connection connection = getConnectionCurrent(context);
            if (connection != null) {
                injectConnection(connection, context);
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        Metadata metadata = getMetadata(context);
        if (metadata.runMode() == ContainerMode.PER_METHOD) {
            ExtensionContext.Store storage = getStorage(context);
            final ExtensionContainer<Container, Connection> extensionContainer = storage.get(metadata.runMode(),
                    ExtensionContainer.class);
            if (extensionContainer != null) {
                logger.debug("Stopping in mode '{}' container: {}",
                        metadata.runMode(), extensionContainer.container().getDockerImageName());
                extensionContainer.stop();
                logger.info("Stopped in mode '{}' container: {}",
                        metadata.runMode(), extensionContainer.container().getDockerImageName());

                storage.remove(getConnectionType());
                storage.remove(metadata.runMode());
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        ExtensionContext.Store storage = getStorage(context);
        Metadata metadata = getMetadata(context);
        if (metadata.runMode() == ContainerMode.PER_CLASS) {
            final ExtensionContainer<Container, Connection> extensionContainer = storage.get(metadata.runMode(),
                    ExtensionContainer.class);
            if (extensionContainer != null) {
                logger.debug("Stopping in mode '{}' container: {}",
                        metadata.runMode(), extensionContainer.container().getDockerImageName());
                extensionContainer.stop();
                logger.info("Stopped in mode '{}' container: {}",
                        metadata.runMode(), extensionContainer.container().getDockerImageName());
            }
        }
    }
}
