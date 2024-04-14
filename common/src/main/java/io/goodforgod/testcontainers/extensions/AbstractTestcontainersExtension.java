package io.goodforgod.testcontainers.extensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

    static final Map<String, Map<SharedKey, ContainerContext<?>>> CLASS_TO_SHARED_CONTAINERS = new ConcurrentHashMap<>();

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected abstract Class<? extends Annotation> getContainerAnnotation();

    protected abstract Class<? extends Annotation> getConnectionAnnotation();

    protected abstract Class<Connection> getConnectionType();

    protected abstract Class<Container> getContainerType();

    protected abstract Optional<Metadata> findMetadata(ExtensionContext context);

    protected final Metadata getMetadata(ExtensionContext context) {
        return findMetadata(context).orElseThrow(() -> new ExtensionConfigurationException("Extension annotation not found"));
    }

    protected abstract ExtensionContext.Namespace getNamespace();

    protected abstract Container createContainerDefault(Metadata metadata);

    protected abstract ContainerContext<Connection> createContainerContext(Container container);

    protected final ExtensionContext.Store getStorage(ExtensionContext context) {
        if (context.getParent().isPresent() && context.getParent().get().getParent().isPresent()) {
            return context.getParent().get().getStore(getNamespace());
        } else if (context.getParent().isEmpty()) {
            return context.getStore(getNamespace());
        } else {
            return context.getStore(getNamespace());
        }
    }

    protected ContainerContext<Connection> getContainerContext(ExtensionContext context) {
        Metadata metadata = getMetadata(context);
        return getStorage(context).get(metadata.runMode(), ContainerContext.class);
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

    protected Optional<Container> findContainerFromField(ExtensionContext context) {
        logger.debug("Looking for {} Container...", getContainerType().getSimpleName());
        if (context.getTestClass().isEmpty() || context.getTestInstance().isEmpty()) {
            return Optional.empty();
        }

        final Optional<Container> container = findContainerInClassField(context.getTestInstance().get());
        if (container.isPresent()) {
            return container;
        } else if (context.getTestClass().filter(c -> c.isAnnotationPresent(Nested.class)).isPresent()) {
            return findParentTestClassIfNested(context).flatMap(this::findContainerInClassField);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<Object> findParentTestClassIfNested(ExtensionContext context) {
        if (context.getTestClass().filter(c -> c.isAnnotationPresent(Nested.class)).isPresent()) {
            return context.getTestInstance()
                    .flatMap(instance -> findParentTestClass(instance.getClass(), context)
                            .flatMap(aClass -> Arrays.stream(instance.getClass().getDeclaredFields())
                                    .filter(f -> f.getType().equals(aClass))
                                    .findFirst()
                                    .map(f -> {
                                        try {
                                            f.setAccessible(true);
                                            return f.get(instance);
                                        } catch (IllegalAccessException e) {
                                            throw new IllegalStateException(e);
                                        }
                                    })));
        }

        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private Optional<Container> findContainerInClassField(Object testClassInstance) {
        return ReflectionUtils.findFields(testClassInstance.getClass(),
                f -> !f.isSynthetic() && f.getAnnotation(getContainerAnnotation()) != null,
                ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .stream()
                .findFirst()
                .map(field -> {
                    try {
                        field.setAccessible(true);
                        Object possibleContainer = field.get(testClassInstance);
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
                });
    }

    private static Optional<Class<?>> findParentTestClass(Class<?> childTestClass, ExtensionContext context) {
        return context.getTestClass()
                .filter(c -> !c.equals(childTestClass))
                .or(() -> context.getParent()
                        .flatMap(parentContext -> findParentTestClass(childTestClass, parentContext)));
    }

    protected void injectContext(ContainerContext<Connection> containerContext, ExtensionContext context) {
        context.getTestInstance().ifPresent(instance -> injectContextIntoInstance(containerContext, instance));

        if (context.getTestClass().filter(c -> c.isAnnotationPresent(Nested.class)).isPresent()) {
            findParentTestClassIfNested(context).ifPresent(instance -> injectContextIntoInstance(containerContext, instance));
        }
    }

    protected void injectContextIntoInstance(ContainerContext<Connection> containerContext, Object testClassInstance) {
        Class<? extends Annotation> connectionAnnotation = getConnectionAnnotation();
        List<Field> connectionFields = ReflectionUtils.findFields(testClassInstance.getClass(),
                f -> !f.isSynthetic()
                        && !Modifier.isFinal(f.getModifiers())
                        && !Modifier.isStatic(f.getModifiers())
                        && f.getAnnotation(connectionAnnotation) != null,
                ReflectionUtils.HierarchyTraversalMode.TOP_DOWN);

        logger.debug("Starting field injection for connection: {}", containerContext.connection());
        for (Field field : connectionFields) {
            injectContextIntoField(containerContext, field, testClassInstance);
        }
    }

    protected void injectContextIntoField(ContainerContext<Connection> containerContext, Field field, Object testClassInstance) {
        try {
            field.setAccessible(true);
            field.set(testClassInstance, containerContext.connection());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(String.format("Field '%s' annotated with @%s can't set connection",
                    field.getName(), getConnectionAnnotation().getSimpleName()), e);
        }
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
        ContainerContext<Connection> containerContext = getContainerContext(extensionContext);
        if (containerContext != null) {
            return containerContext.connection();
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

            return Optional.ofNullable(getContainerContext(extensionContext).connection())
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
        if (getContainerContext(context) == null) {
            if (metadata.runMode() == ContainerMode.PER_RUN) {
                final Optional<Container> containerFromField = findContainerFromField(context);
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

                var containerContext = sharedContainerMap.computeIfAbsent(sharedKey, k -> {
                    Container container = containerFromField.orElseGet(() -> {
                        logger.debug("Getting default container for image: {}", metadata.image());
                        return createContainerDefault(metadata);
                    });

                    container.withReuse(true);
                    ContainerContext<Connection> conContext = createContainerContext(container);
                    logger.debug("Starting in mode '{}' container: {}", metadata.runMode(), conContext);
                    conContext.start();
                    logger.info("Started in mode '{}' container: {}", metadata.runMode(), conContext);
                    return conContext;
                });

                storage.put(metadata.runMode(), containerContext);
                injectContext((ContainerContext<Connection>) containerContext, context);
            } else if (metadata.runMode() == ContainerMode.PER_CLASS) {
                Container container = findContainerFromField(context).orElseGet(() -> {
                    logger.debug("Getting default container for image: {}", metadata.image());
                    return createContainerDefault(metadata);
                });

                ContainerContext<Connection> containerContext = createContainerContext(container);
                logger.debug("Starting in mode '{}' container: {}", metadata.runMode(), containerContext);
                containerContext.start();
                logger.info("Started in mode '{}' container: {}", metadata.runMode(), containerContext);
                storage.put(metadata.runMode(), containerContext);
                injectContext(containerContext, context);
            }
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        Metadata metadata = getMetadata(context);
        ExtensionContext.Store storage = getStorage(context);
        if (getContainerContext(context) == null) {
            if (metadata.runMode() == ContainerMode.PER_METHOD) {
                Container container = findContainerFromField(context).orElseGet(() -> {
                    logger.debug("Getting default container for image: {}", metadata.image());
                    return createContainerDefault(metadata);
                });

                ContainerContext<Connection> containerContext = createContainerContext(container);
                logger.debug("Starting in mode '{}' container: {}", metadata.runMode(), containerContext);
                container.start();
                logger.info("Started in mode '{}' container: {}", metadata.runMode(), containerContext);
                storage.put(metadata.runMode(), containerContext);
            }
        }

        TestInstance.Lifecycle lifecycle = context.getTestInstanceLifecycle().orElse(TestInstance.Lifecycle.PER_METHOD);
        if (lifecycle == TestInstance.Lifecycle.PER_METHOD) {
            var containerContext = getContainerContext(context);
            if (containerContext != null) {
                injectContext(containerContext, context);
            }
        } else if (metadata.runMode() == ContainerMode.PER_METHOD) {
            var containerContext = getContainerContext(context);
            if (containerContext != null) {
                injectContext(containerContext, context);
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        Metadata metadata = getMetadata(context);
        if (metadata.runMode() == ContainerMode.PER_METHOD) {
            ExtensionContext.Store storage = getStorage(context);
            final ContainerContext<Connection> containerContext = getContainerContext(context);
            if (containerContext != null) {
                logger.debug("Stopping in mode '{}' container: {}", metadata.runMode(), containerContext);
                containerContext.stop();
                logger.info("Stopped in mode '{}' container: {}", metadata.runMode(), containerContext);

                storage.remove(getConnectionType());
                storage.remove(metadata.runMode());
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        Metadata metadata = getMetadata(context);
        if (metadata.runMode() == ContainerMode.PER_CLASS) {
            final ContainerContext<Connection> containerContext = getContainerContext(context);
            if (containerContext != null) {
                logger.debug("Stopping in mode '{}' container: {}", metadata.runMode(), containerContext);
                containerContext.stop();
                logger.info("Stopped in mode '{}' container: {}", metadata.runMode(), containerContext);
            }
        }
    }
}
