package io.goodforgod.testcontainers.extensions.kafka;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@Internal
final class TestcontainersKafkaExtension implements
        BeforeAllCallback,
        BeforeEachCallback,
        AfterAllCallback,
        AfterEachCallback,
        ParameterResolver {

    private static final String EXTERNAL_TEST_KAFKA_BOOTSTRAP = "EXTERNAL_TEST_KAFKA_BOOTSTRAP_SERVERS";
    private static final String EXTERNAL_TEST_KAFKA_PREFIX = "EXTERNAL_TEST_KAFKA_";

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersKafkaExtension.class);

    private static final Map<String, ExtensionContainerImpl> IMAGE_TO_SHARED_CONTAINER = new ConcurrentHashMap<>();
    private Properties externalConnection = null;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final class KafkaConnectionPool {

        private final List<KafkaConnectionImpl> connections = new ArrayList<>();

        private void add(KafkaConnectionImpl connection) {
            connections.add(connection);
        }

        private void clear() {
            for (KafkaConnectionImpl connection : connections) {
                try {
                    connection.clear();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }

        private void close() {
            for (KafkaConnectionImpl connection : connections) {
                try {
                    connection.close();
                } catch (Exception e) {
                    // do nothing
                }
            }

            connections.clear();
        }
    }

    private static final class ExtensionContainerImpl implements ExtensionContainer {

        private final KafkaContainer container;
        private final Properties properties;

        ExtensionContainerImpl(KafkaContainer container, Properties properties) {
            this.container = container;
            this.properties = properties;
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

    private <T extends Annotation> Optional<T> findAnnotation(Class<T> annotationType, ExtensionContext context) {
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

    private Optional<KafkaContainer> getContainerFromField(ExtensionContext context) {
        logger.debug("Looking for Kafka Container...");
        final Class<? extends Annotation> containerAnnotation = getAnnotationContainer();
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
                                    logger.debug("Found Kafka Container in field: {}", field.getName());
                                    return ((KafkaContainer) possibleContainer);
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

    private Class<KafkaContainer> getContainerType() {
        return KafkaContainer.class;
    }

    private Class<? extends Annotation> getAnnotationContainer() {
        return ContainerKafka.class;
    }

    private Class<? extends Annotation> getAnnotationConnection() {
        return ContainerKafkaConnection.class;
    }

    @NotNull
    private KafkaContainer getDefaultContainer(@NotNull ContainerMetadata metadata) {
        var dockerImage = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("confluentinc/cp-kafka"));

        var alias = "kafka-" + System.currentTimeMillis();
        return new KafkaContainer(dockerImage)
                .withEnv("KAFKA_CONFLUENT_SUPPORT_METRICS_ENABLE", "false")
                .withEnv("AUTO_CREATE_TOPICS", "true")
                .withNetworkAliases(alias)
                .withNetwork(Network.SHARED)
                .withEmbeddedZookeeper()
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(KafkaContainer.class)))
                .withStartupTimeout(Duration.ofMinutes(3));
    }

    @NotNull
    private Optional<ContainerMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersKafka.class, context)
                .map(a -> new ContainerMetadata(a.image(), a.mode()));
    }

    @NotNull
    private Properties getPropertiesForContainer(@NotNull KafkaContainer container) {
        final Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers());
        return properties;
    }

    @NotNull
    private Optional<Properties> getConnectionExternal() {
        var bootstrap = System.getenv(EXTERNAL_TEST_KAFKA_BOOTSTRAP);
        if (bootstrap != null) {
            final Properties properties = new Properties();
            System.getenv().forEach((k, v) -> {
                if (k.startsWith(EXTERNAL_TEST_KAFKA_PREFIX)) {
                    var name = k.replace(EXTERNAL_TEST_KAFKA_PREFIX, "").replace("_", ".").toLowerCase();
                    properties.put(name, v);
                }
            });

            return Optional.of(properties);
        } else {
            return Optional.empty();
        }
    }

    @Nullable
    private Properties getPropertiesExternalCached() {
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

    private void injectKafkaConnection(Properties containerProperties, ExtensionContext context) {
        var annotationProducer = getAnnotationConnection();
        var connectionFields = ReflectionUtils.findFields(context.getRequiredTestClass(),
                f -> !f.isSynthetic()
                        && !Modifier.isFinal(f.getModifiers())
                        && !Modifier.isStatic(f.getModifiers())
                        && f.getAnnotation(annotationProducer) != null,
                ReflectionUtils.HierarchyTraversalMode.TOP_DOWN);

        logger.debug("Starting @ContainerKafkaConnection field injection for container properties: {}", containerProperties);

        var storage = context.getStore(NAMESPACE);
        var pool = storage.get(KafkaConnectionPool.class, KafkaConnectionPool.class);
        context.getTestInstance().ifPresent(instance -> {
            for (Field field : connectionFields) {
                try {
                    final ContainerKafkaConnection annotation = field.getAnnotation(ContainerKafkaConnection.class);
                    final Properties fieldProperties = new Properties();
                    fieldProperties.putAll(containerProperties);
                    Arrays.stream(annotation.properties())
                            .forEach(property -> fieldProperties.put(property.name(), property.value()));

                    var kafkaConnection = new KafkaConnectionImpl(fieldProperties);
                    pool.add(kafkaConnection);

                    field.setAccessible(true);
                    field.set(instance, kafkaConnection);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(String.format("Field '%s' annotated with @%s can't set kafka connection",
                            field.getName(), annotationProducer.getSimpleName()), e);
                }
            }
        });
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        var metadata = getMetadata(context);
        var storage = context.getStore(NAMESPACE);
        storage.put(KafkaConnectionPool.class, new KafkaConnectionPool());

        var externalProperties = getPropertiesExternalCached();
        if (externalProperties != null) {
            injectKafkaConnection(externalProperties, context);
            return;
        }

        if (metadata.runMode() == ContainerMode.PER_RUN) {
            var containerFromField = getContainerFromField(context);
            var imageToLook = containerFromField.map(GenericContainer::getDockerImageName).orElseGet(metadata::image);

            var extensionContainer = IMAGE_TO_SHARED_CONTAINER.computeIfAbsent(imageToLook, k -> {
                var container = containerFromField.orElseGet(() -> {
                    logger.debug("Getting default Kafka Container for image: {}", metadata.image());
                    return getDefaultContainer(metadata);
                });

                logger.debug("Starting in mode '{}' Kafka Container: {}", metadata.runMode(), container.getDockerImageName());
                container.withReuse(true).start();
                logger.debug("Started successfully in mode '{}' Kafka Container: {}", metadata.runMode(),
                        container.getDockerImageName());
                return new ExtensionContainerImpl(container, getPropertiesForContainer(container));
            });

            storage.put(ContainerMode.PER_RUN, extensionContainer);
            injectKafkaConnection(extensionContainer.properties, context);
        } else if (metadata.runMode() == ContainerMode.PER_CLASS) {
            var container = getContainerFromField(context).orElseGet(() -> {
                logger.debug("Getting default Kafka Container for image: {}", metadata.image());
                return getDefaultContainer(metadata);
            });

            logger.debug("Starting in mode '{}' Kafka Container: {}", metadata.runMode(), container.getDockerImageName());
            container.start();
            logger.debug("Started successfully in mode '{}' Kafka Container: {}", metadata.runMode(),
                    container.getDockerImageName());
            var extensionContainer = new ExtensionContainerImpl(container, getPropertiesForContainer(container));
            storage.put(ContainerMode.PER_CLASS, extensionContainer);
            injectKafkaConnection(extensionContainer.properties, context);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        var metadata = getMetadata(context);
        var storage = context.getStore(NAMESPACE);

        var externalProperties = getPropertiesExternalCached();
        if (externalProperties != null) {
            injectKafkaConnection(externalProperties, context);
            return;
        }

        if (metadata.runMode() == ContainerMode.PER_METHOD) {
            var container = getContainerFromField(context).orElseGet(() -> {
                logger.debug("Getting default Kafka Container for image: {}", metadata.image());
                return getDefaultContainer(metadata);
            });

            logger.debug("Starting in mode '{}' Kafka Container: {}", metadata.runMode(), container.getDockerImageName());
            container.start();
            logger.debug("Started successfully in mode '{}' Kafka Container: {}", metadata.runMode(),
                    container.getDockerImageName());

            final ExtensionContainerImpl extensionContainer = new ExtensionContainerImpl(container,
                    getPropertiesForContainer(container));
            storage.put(ContainerMode.PER_METHOD, extensionContainer);
            injectKafkaConnection(extensionContainer.properties, context);
        } else if (metadata.runMode() == ContainerMode.PER_CLASS) {
            var extensionContainer = storage.get(ContainerMode.PER_CLASS, ExtensionContainerImpl.class);
            injectKafkaConnection(extensionContainer.properties, context);
        } else if (metadata.runMode() == ContainerMode.PER_RUN) {
            var extensionContainer = storage.get(ContainerMode.PER_RUN, ExtensionContainerImpl.class);
            injectKafkaConnection(extensionContainer.properties, context);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        var metadata = getMetadata(context);
        var storage = context.getStore(NAMESPACE);

        if (metadata.runMode() == ContainerMode.PER_METHOD) {
            var extensionContainer = storage.get(ContainerMode.PER_METHOD, ExtensionContainerImpl.class);
            if (extensionContainer != null) {
                logger.debug("Stopping in mode '{}' Kafka Container: {}", metadata.runMode(),
                        extensionContainer.container.getDockerImageName());
                extensionContainer.stop();
                logger.debug("Stopped successfully in mode '{}' Kafka Container: {}", metadata.runMode(),
                        extensionContainer.container.getDockerImageName());
            }
        }

        var pool = storage.get(KafkaConnectionPool.class, KafkaConnectionPool.class);
        pool.clear();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        var metadata = getMetadata(context);
        var storage = context.getStore(NAMESPACE);

        if (metadata.runMode() == ContainerMode.PER_CLASS) {
            var extensionContainer = storage.get(ContainerMode.PER_CLASS, ExtensionContainerImpl.class);
            if (extensionContainer != null) {
                logger.debug("Stopping in mode '{}' Kafka Container: {}", metadata.runMode(),
                        extensionContainer.container.getDockerImageName());
                extensionContainer.stop();
                logger.debug("Stopped successfully in mode '{}' Kafka Container: {}", metadata.runMode(),
                        extensionContainer.container.getDockerImageName());
            }
        }

        var pool = storage.get(KafkaConnectionPool.class, KafkaConnectionPool.class);
        pool.close();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context)
            throws ParameterResolutionException {
        final Class<? extends Annotation> connectionAnnotation = getAnnotationConnection();
        final boolean foundSuitable = parameterContext.getDeclaringExecutable() instanceof Method
                && parameterContext.getParameter().getAnnotation(connectionAnnotation) != null;

        if (!foundSuitable) {
            return false;
        }

        if (!parameterContext.getParameter().getType().equals(KafkaConnection.class)) {
            throw new ExtensionConfigurationException(String.format("Parameter '%s' annotated @%s is not of type %s",
                    parameterContext.getParameter().getName(), connectionAnnotation.getSimpleName(),
                    KafkaConnection.class));
        }

        return true;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context)
            throws ParameterResolutionException {
        var storage = context.getStore(NAMESPACE);
        var pool = storage.get(KafkaConnectionPool.class, KafkaConnectionPool.class);
        var metadata = getMetadata(context);

        final ContainerKafkaConnection annotation = parameterContext.getParameter().getAnnotation(ContainerKafkaConnection.class);
        var parameterProperties = new Properties();

        var externalProperties = getPropertiesExternalCached();
        if (externalProperties != null) {
            parameterProperties.putAll(externalProperties);
        } else if (metadata.runMode() == ContainerMode.PER_METHOD) {
            var extensionContainer = storage.get(ContainerMode.PER_METHOD, ExtensionContainerImpl.class);
            parameterProperties.putAll(extensionContainer.properties);
        } else if (metadata.runMode() == ContainerMode.PER_CLASS) {
            var extensionContainer = storage.get(ContainerMode.PER_CLASS, ExtensionContainerImpl.class);
            parameterProperties.putAll(extensionContainer.properties);
        } else {
            var extensionContainer = storage.get(ContainerMode.PER_RUN, ExtensionContainerImpl.class);
            parameterProperties.putAll(extensionContainer.properties);
        }

        for (ContainerKafkaConnection.Property property : annotation.properties()) {
            parameterProperties.put(property.name(), property.value());
        }

        var kafkaConnection = new KafkaConnectionImpl(parameterProperties);
        pool.add(kafkaConnection);
        return kafkaConnection;
    }
}
