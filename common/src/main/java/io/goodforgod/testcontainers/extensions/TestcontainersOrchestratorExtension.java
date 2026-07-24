package io.goodforgod.testcontainers.extensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

@Internal
public final class TestcontainersOrchestratorExtension
        implements
        BeforeAllCallback,
        BeforeEachCallback,
        AfterAllCallback,
        AfterEachCallback,
        ParameterResolver {

    private record SharedKey(Class<?> provider, String image, boolean network, String alias, Object networkInstance) {}

    private record ActiveProvider<A extends Annotation, C> (TestcontainersProvider<A, C> provider, A annotation) {}

    private static final class ActiveContext<C> {

        private final TestcontainersProvider<?, C> provider;
        private final Annotation annotation;
        private final ContainerMode mode;
        private final ContainerContext<C> context;

        private ActiveContext(TestcontainersProvider<?, C> provider,
                              Annotation annotation,
                              ContainerMode mode,
                              ContainerContext<C> context) {
            this.provider = provider;
            this.annotation = annotation;
            this.mode = mode;
            this.context = context;
        }
    }

    private static final class OrchestrationState {

        private final Map<Class<? extends Annotation>, ActiveContext<?>> contexts = new LinkedHashMap<>();
        private final Set<ContainerMode> modesStarted = EnumSet.noneOf(ContainerMode.class);
        private final Set<String> beforeEachHooked = new HashSet<>();
        private final Set<String> afterEachHooked = new HashSet<>();
    }

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersOrchestratorExtension.class);

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "testcontainers-orchestrator");
        thread.setDaemon(true);
        return thread;
    });

    private static final Map<SharedKey, ActiveContext<?>> SHARED_PER_RUN = new ConcurrentHashMap<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final List<TestcontainersProvider<?, ?>> providers = loadProviders();

    @Override
    public void beforeAll(ExtensionContext context) {
        start(context, ContainerMode.PER_RUN, ContainerMode.PER_CLASS);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        start(context, ContainerMode.PER_RUN, ContainerMode.PER_CLASS);
        start(context, ContainerMode.PER_METHOD);
        OrchestrationState state = state(context);
        if (state.beforeEachHooked.add(context.getUniqueId())) {
            runContextHooks(context, new ArrayList<>(state.contexts.values()), false,
                    TestcontainersOrchestratorExtension::beforeEachUnchecked);
        }
        injectAll(context);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        OrchestrationState state = state(context);
        if (state.afterEachHooked.add(context.getUniqueId())) {
            runContextHooks(context, new ArrayList<>(state.contexts.values()), true,
                    TestcontainersOrchestratorExtension::afterEachUnchecked);
        }
        stop(context, ContainerMode.PER_METHOD);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        stop(context, ContainerMode.PER_CLASS);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return findProviderForParameter(parameterContext)
                .map(provider -> {
                    if (!parameterContext.getParameter().getType().equals(provider.connectionType())) {
                        throw new ParameterResolutionException(String.format("Parameter '%s' annotated @%s is not of type %s",
                                parameterContext.getParameter().getName(),
                                provider.connectionAnnotationType().getSimpleName(),
                                provider.connectionType()));
                    }

                    return true;
                })
                .orElse(false);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        TestcontainersProvider<?, ?> provider = findProviderForParameter(parameterContext)
                .orElseThrow(() -> new ParameterResolutionException("Connection provider not found"));

        ActiveProvider<?, ?> activeProvider = findActiveProvider(provider.annotationType(), extensionContext)
                .orElseThrow(() -> new ParameterResolutionException(
                        "Provider annotation not found for " + provider.connectionAnnotationType().getSimpleName()));

        ContainerMode mode = mode(activeProvider);
        if (mode == ContainerMode.PER_METHOD) {
            start(extensionContext, ContainerMode.PER_METHOD);
            OrchestrationState state = state(extensionContext);
            if (state.beforeEachHooked.add(extensionContext.getUniqueId())) {
                state.contexts.values().forEach(active -> beforeEachUnchecked(active, extensionContext));
            }
        } else {
            start(extensionContext, ContainerMode.PER_RUN, ContainerMode.PER_CLASS);
        }

        ActiveContext<?> active = state(extensionContext).contexts.get(provider.annotationType());
        if (active == null) {
            throw new ParameterResolutionException("Container context not started for " + provider.annotationType().getName());
        }

        return resolveUnchecked(active, parameterContext);
    }

    static void stopPerRun() {
        SHARED_PER_RUN.values().forEach(TestcontainersOrchestratorExtension::stopUnchecked);
        SHARED_PER_RUN.clear();
    }

    private void start(ExtensionContext context, ContainerMode... modes) {
        if (context.getTestClass().isEmpty()) {
            return;
        }

        OrchestrationState state = state(context);
        EnumSet<ContainerMode> requested = EnumSet.noneOf(ContainerMode.class);
        requested.addAll(Arrays.asList(modes));
        requested.removeAll(state.modesStarted);
        if (requested.isEmpty()) {
            return;
        }

        List<ActiveProvider<?, ?>> activeProviders = new ArrayList<>();
        for (TestcontainersProvider<?, ?> provider : providers) {
            findActiveProvider(provider.annotationType(), context)
                    .filter(activeProvider -> requested.contains(mode(activeProvider)))
                    .ifPresent(activeProviders::add);
        }
        if (activeProviders.isEmpty()) {
            state.modesStarted.addAll(requested);
            return;
        }

        List<ActiveContext<?>> started = startAll(context, state, activeProviders);
        started.forEach(active -> state.contexts.put(active.provider.annotationType(), active));

        runContextHooks(context, started, false, TestcontainersOrchestratorExtension::afterStartUnchecked);

        injectAll(context);
        state.modesStarted.addAll(requested);
    }

    private <A extends Annotation, C> ActiveContext<C> startOne(ExtensionContext context, ActiveProvider<A, C> active) {
        TestcontainersProvider<A, C> provider = active.provider();
        A annotation = active.annotation();
        ContainerMode mode = provider.mode(annotation);

        if (mode == ContainerMode.PER_RUN) {
            Optional<org.testcontainers.containers.Network> network = findNetworkFromField(context);
            SharedKey key = new SharedKey(provider.getClass(), provider.image(annotation),
                    provider.networkShared(annotation), provider.networkAlias(annotation), network.orElse(null));
            return (ActiveContext<C>) SHARED_PER_RUN.computeIfAbsent(key, ignored -> createAndStart(context, active));
        }

        return createAndStart(context, active);
    }

    private <A extends Annotation, C> ActiveContext<C> createAndStart(ExtensionContext context, ActiveProvider<A, C> active) {
        TestcontainersProvider<A, C> provider = active.provider();
        A annotation = active.annotation();
        GenericContainer<?> container = findContainerFromField(context, provider)
                .orElseGet(() -> provider.createContainer(annotation));
        configureNetwork(context, provider, annotation, container);
        if (provider.mode(annotation) == ContainerMode.PER_RUN) {
            container.withReuse(true);
        }

        ContainerContext<C> containerContext = provider.createContext(container);

        logger.debug("Starting in mode '{}' container: {}", provider.mode(annotation), containerContext);
        containerContext.start();
        logger.info("Started in mode '{}' container: {}", provider.mode(annotation), containerContext);
        return new ActiveContext<>(provider, annotation, provider.mode(annotation), containerContext);
    }

    private void stop(ExtensionContext context, ContainerMode mode) {
        OrchestrationState state = state(context);
        List<ActiveContext<?>> stopping = state.contexts.values().stream()
                .filter(active -> active.mode == mode)
                .toList();
        runContextHooks(context, stopping, true, TestcontainersOrchestratorExtension::beforeStopUnchecked);
        runContextHooks(context, stopping, true, (active, ignored) -> stopUnchecked(active));
        state.contexts.values().removeIf(active -> active.mode == mode);
        state.modesStarted.remove(mode);
    }

    private void injectAll(ExtensionContext context) {
        context.getTestInstance().ifPresent(instance -> {
            state(context).contexts.values().forEach(active -> injectIntoInstance(active, instance));
            findParentTestClassIfNested(context)
                    .ifPresent(parent -> state(context).contexts.values()
                            .forEach(active -> injectIntoInstance(active, parent)));
        });
    }

    private void injectIntoInstance(ActiveContext<?> active, Object instance) {
        List<Field> fields = ReflectionUtils.findFields(instance.getClass(),
                field -> !field.isSynthetic()
                        && !Modifier.isFinal(field.getModifiers())
                        && !Modifier.isStatic(field.getModifiers())
                        && field.getAnnotation(active.provider.connectionAnnotationType()) != null,
                ReflectionUtils.HierarchyTraversalMode.TOP_DOWN);

        fields.forEach(field -> injectUnchecked(active, field, instance));
    }

    private Optional<GenericContainer<?>> findContainerFromField(ExtensionContext context,
                                                                 TestcontainersProvider<?, ?> provider) {
        List<Object> instances = new ArrayList<>();
        context.getTestInstance().ifPresent(instance -> {
            instances.add(instance);
            findParentTestClassIfNested(context).ifPresent(instances::add);
        });

        for (Object instance : instances) {
            Optional<GenericContainer<?>> container = findContainerInInstance(instance, provider);
            if (container.isPresent()) {
                return container;
            }
        }

        return findContainerStatic(context, provider);
    }

    private Optional<org.testcontainers.containers.Network> findNetworkFromField(ExtensionContext context) {
        List<Object> instances = new ArrayList<>();
        context.getTestInstance().ifPresent(instance -> {
            instances.add(instance);
            findParentTestClassIfNested(context).ifPresent(instances::add);
        });

        for (Object instance : instances) {
            Optional<org.testcontainers.containers.Network> network = findNetworkInInstance(instance);
            if (network.isPresent()) {
                return network;
            }
        }

        return findNetworkStatic(context);
    }

    private Optional<GenericContainer<?>> findContainerInInstance(Object instance, TestcontainersProvider<?, ?> provider) {
        return ReflectionUtils.findFields(instance.getClass(),
                field -> !field.isSynthetic()
                        && !Modifier.isStatic(field.getModifiers())
                        && field.getAnnotation(provider.containerAnnotationType()) != null,
                ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .stream()
                .findFirst()
                .map(field -> getContainerFromField(field, instance, provider));
    }

    private Optional<org.testcontainers.containers.Network> findNetworkInInstance(Object instance) {
        return ReflectionUtils.findFields(instance.getClass(),
                field -> !field.isSynthetic()
                        && !Modifier.isStatic(field.getModifiers())
                        && field.getAnnotation(ContainerNetwork.class) != null,
                ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .stream()
                .findFirst()
                .map(field -> getNetworkFromField(field, instance));
    }

    private Optional<GenericContainer<?>> findContainerStatic(ExtensionContext context, TestcontainersProvider<?, ?> provider) {
        Optional<ExtensionContext> current = Optional.of(context);
        while (current.isPresent()) {
            Optional<GenericContainer<?>> container = current.get().getTestClass()
                    .flatMap(testClass -> findContainerStatic(testClass, provider));
            if (container.isPresent()) {
                return container;
            }

            current = current.get().getParent();
        }

        return Optional.empty();
    }

    private Optional<GenericContainer<?>> findContainerStatic(Class<?> testClass, TestcontainersProvider<?, ?> provider) {
        Class<?> current = testClass;
        while (!current.equals(Object.class)) {
            Optional<GenericContainer<?>> container = ReflectionUtils.findFields(current,
                    field -> !field.isSynthetic()
                            && Modifier.isStatic(field.getModifiers())
                            && field.getAnnotation(provider.containerAnnotationType()) != null,
                    ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                    .stream()
                    .findFirst()
                    .map(field -> getContainerFromField(field, null, provider));
            if (container.isPresent()) {
                return container;
            }

            current = current.getSuperclass();
        }

        return Optional.empty();
    }

    private Optional<org.testcontainers.containers.Network> findNetworkStatic(ExtensionContext context) {
        Optional<ExtensionContext> current = Optional.of(context);
        while (current.isPresent()) {
            Optional<org.testcontainers.containers.Network> network = current.get().getTestClass()
                    .flatMap(this::findNetworkStatic);
            if (network.isPresent()) {
                return network;
            }

            current = current.get().getParent();
        }

        return Optional.empty();
    }

    private Optional<org.testcontainers.containers.Network> findNetworkStatic(Class<?> testClass) {
        Class<?> current = testClass;
        while (!current.equals(Object.class)) {
            Optional<org.testcontainers.containers.Network> network = ReflectionUtils.findFields(current,
                    field -> !field.isSynthetic()
                            && Modifier.isStatic(field.getModifiers())
                            && field.getAnnotation(ContainerNetwork.class) != null,
                    ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                    .stream()
                    .findFirst()
                    .map(field -> getNetworkFromField(field, null));
            if (network.isPresent()) {
                return network;
            }

            current = current.getSuperclass();
        }

        return Optional.empty();
    }

    private GenericContainer<?> getContainerFromField(Field field, Object instance, TestcontainersProvider<?, ?> provider) {
        try {
            field.setAccessible(true);
            Object value = field.get(instance);
            if (value instanceof GenericContainer<?> container) {
                return container;
            }

            throw new IllegalArgumentException("Field '%s' annotated with @%s value must be GenericContainer"
                    .formatted(field.getName(), provider.containerAnnotationType().getSimpleName()));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private org.testcontainers.containers.Network getNetworkFromField(Field field, Object instance) {
        try {
            field.setAccessible(true);
            Object value = field.get(instance);
            if (value instanceof org.testcontainers.containers.Network network) {
                return network;
            }

            throw new IllegalArgumentException("Field '%s' annotated with @%s value must be %s"
                    .formatted(field.getName(), ContainerNetwork.class.getSimpleName(),
                            org.testcontainers.containers.Network.class.getName()));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Optional<Object> findParentTestClassIfNested(ExtensionContext context) {
        if (context.getTestClass().filter(c -> c.isAnnotationPresent(Nested.class)).isEmpty()) {
            return Optional.empty();
        }

        return context.getTestInstance()
                .flatMap(instance -> findParentTestClass(instance.getClass(), context)
                        .flatMap(aClass -> Arrays.stream(instance.getClass().getDeclaredFields())
                                .filter(f -> f.getType().equals(aClass))
                                .findFirst()
                                .map(field -> {
                                    try {
                                        field.setAccessible(true);
                                        return field.get(instance);
                                    } catch (IllegalAccessException e) {
                                        throw new IllegalStateException(e);
                                    }
                                })));
    }

    private static Optional<Class<?>> findParentTestClass(Class<?> childTestClass, ExtensionContext context) {
        return context.getTestClass()
                .filter(c -> !c.equals(childTestClass))
                .or(() -> context.getParent()
                        .flatMap(parentContext -> findParentTestClass(childTestClass, parentContext)));
    }

    private Optional<TestcontainersProvider<?, ?>> findProviderForParameter(ParameterContext parameterContext) {
        return providers.stream()
                .filter(provider -> parameterContext.getParameter().getAnnotation(provider.connectionAnnotationType()) != null)
                .findFirst();
    }

    private <A extends Annotation> Optional<ActiveProvider<A, ?>> findActiveProvider(Class<A> annotationType,
                                                                                     ExtensionContext context) {
        return findAnnotation(annotationType, context)
                .map(annotation -> providers.stream()
                        .filter(provider -> provider.annotationType().equals(annotationType))
                        .findFirst()
                        .map(provider -> new ActiveProvider<>((TestcontainersProvider<A, ?>) provider, annotation)))
                .flatMap(a -> a);
    }

    private static <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType, ExtensionContext context) {
        Optional<ExtensionContext> current = Optional.of(context);
        while (current.isPresent()) {
            if (current.get().getTestClass().isEmpty()) {
                current = current.get().getParent();
                continue;
            }

            Class<?> requiredClass = current.get().getRequiredTestClass();
            while (!requiredClass.equals(Object.class)) {
                Optional<A> annotation = AnnotationSupport.findAnnotation(requiredClass, annotationType);
                if (annotation.isPresent()) {
                    return annotation;
                }

                requiredClass = requiredClass.getSuperclass();
            }

            current = current.get().getParent();
        }

        return Optional.empty();
    }

    private OrchestrationState state(ExtensionContext context) {
        return getStore(context).getOrComputeIfAbsent(OrchestrationState.class);
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        if (context.getParent().isPresent() && context.getParent().get().getParent().isPresent()) {
            return context.getParent().get().getStore(NAMESPACE);
        } else {
            return context.getStore(NAMESPACE);
        }
    }

    private static ContainerMode mode(ActiveProvider<?, ?> provider) {
        return modeUnchecked(provider);
    }

    private static <A extends Annotation> ContainerMode modeUnchecked(ActiveProvider<A, ?> provider) {
        return provider.provider().mode(provider.annotation());
    }

    private List<ActiveContext<?>> startAll(ExtensionContext context,
                                            OrchestrationState state,
                                            List<ActiveProvider<?, ?>> activeProviders) {
        List<ActiveContext<?>> started = new ArrayList<>();
        Set<Class<? extends Annotation>> available = new HashSet<>(state.contexts.keySet());
        Set<Class<? extends Annotation>> activeAnnotations = new HashSet<>();
        activeProviders.forEach(provider -> activeAnnotations.add(provider.provider.annotationType()));

        List<ActiveProvider<?, ?>> remaining = new ArrayList<>(activeProviders);
        while (!remaining.isEmpty()) {
            List<ActiveProvider<?, ?>> batch = remaining.stream()
                    .filter(provider -> dependencies(provider).stream()
                            .allMatch(dependency -> available.contains(dependency) || !activeAnnotations.contains(dependency)))
                    .toList();
            validateDependencies(batch, remaining, available, activeAnnotations);
            List<ActiveContext<?>> batchStarted = runStartBatch(context, batch);
            started.addAll(batchStarted);
            batchStarted.forEach(active -> available.add(active.provider.annotationType()));
            remaining.removeAll(batch);
        }

        return started;
    }

    private List<ActiveContext<?>> runStartBatch(ExtensionContext context, List<ActiveProvider<?, ?>> batch) {
        List<CompletableFuture<ActiveContext<?>>> futures = new ArrayList<>();
        for (ActiveProvider<?, ?> activeProvider : batch) {
            futures.add(CompletableFuture.supplyAsync(() -> (ActiveContext<?>) startOne(context, activeProvider), EXECUTOR));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private void runContextHooks(ExtensionContext context,
                                 List<ActiveContext<?>> activeContexts,
                                 boolean reverse,
                                 BiConsumer<ActiveContext<?>, ExtensionContext> hook) {
        Set<Class<? extends Annotation>> activeAnnotations = new HashSet<>();
        activeContexts.forEach(active -> activeAnnotations.add(active.provider.annotationType()));
        Set<Class<? extends Annotation>> done = new HashSet<>();
        List<ActiveContext<?>> remaining = new ArrayList<>(activeContexts);

        while (!remaining.isEmpty()) {
            List<ActiveContext<?>> batch = remaining.stream()
                    .filter(active -> {
                        Set<Class<? extends Annotation>> dependencies = dependencies(active);
                        if (reverse) {
                            return remaining.stream()
                                    .filter(other -> other != active)
                                    .noneMatch(other -> dependencies(other).contains(active.provider.annotationType()));
                        } else {
                            return dependencies.stream()
                                    .allMatch(dependency -> done.contains(dependency) || !activeAnnotations.contains(dependency));
                        }
                    })
                    .toList();
            if (batch.isEmpty()) {
                throw new ExtensionConfigurationException("Cycle detected in Testcontainers provider dependencies");
            }

            runHookBatch(context, batch, hook);
            batch.forEach(active -> done.add(active.provider.annotationType()));
            remaining.removeAll(batch);
        }
    }

    private void runHookBatch(ExtensionContext context,
                              List<ActiveContext<?>> batch,
                              BiConsumer<ActiveContext<?>, ExtensionContext> hook) {
        List<CompletableFuture<Void>> futures = batch.stream()
                .map(active -> CompletableFuture.runAsync(() -> hook.accept(active, context), EXECUTOR))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void validateDependencies(List<ActiveProvider<?, ?>> batch,
                                      List<ActiveProvider<?, ?>> remaining,
                                      Set<Class<? extends Annotation>> available,
                                      Set<Class<? extends Annotation>> activeAnnotations) {
        if (!batch.isEmpty()) {
            return;
        }

        for (ActiveProvider<?, ?> provider : remaining) {
            for (Class<? extends Annotation> dependency : dependencies(provider)) {
                if (!available.contains(dependency) && !activeAnnotations.contains(dependency)) {
                    throw new ExtensionConfigurationException("Provider %s depends on missing annotation %s"
                            .formatted(provider.provider.getClass().getName(), dependency.getName()));
                }
            }
        }

        throw new ExtensionConfigurationException("Cycle detected in Testcontainers provider dependencies");
    }

    private static <A extends Annotation> Set<Class<? extends Annotation>> dependencies(ActiveProvider<A, ?> provider) {
        return provider.provider().dependencies(provider.annotation());
    }

    private static <A extends Annotation> Set<Class<? extends Annotation>> dependencies(ActiveContext<?> active) {
        return ((TestcontainersProvider<A, ?>) active.provider).dependencies((A) active.annotation);
    }

    private <A extends Annotation> void configureNetwork(ExtensionContext context,
                                                         TestcontainersProvider<A, ?> provider,
                                                         A annotation,
                                                         GenericContainer<?> container) {
        String alias = provider.networkAlias(annotation);
        if (alias != null && !alias.isBlank() && !container.getNetworkAliases().contains(alias)) {
            List<String> aliases = new ArrayList<>(container.getNetworkAliases());
            aliases.add(alias);
            container.setNetworkAliases(aliases);
        }

        Optional<org.testcontainers.containers.Network> networkFromField = findNetworkFromField(context);
        if (networkFromField.isPresent()) {
            container.withNetwork(networkFromField.get());
        } else if (provider.networkShared(annotation) && container.getNetwork() == null) {
            container.withNetwork(org.testcontainers.containers.Network.SHARED);
        }
    }

    private static <C> Object resolveUnchecked(ActiveContext<C> active, ParameterContext parameter) {
        return active.provider.resolveParameter(active.context, parameter);
    }

    private static <C> void injectUnchecked(ActiveContext<C> active, Field field, Object instance) {
        active.provider.injectField(active.context, field, instance);
    }

    private static <A extends Annotation, C> void afterStartUnchecked(ActiveContext<C> active, ExtensionContext extension) {
        ((TestcontainersProvider<A, C>) active.provider).afterStart((A) active.annotation, active.context, extension);
    }

    private static <A extends Annotation, C> void beforeStopUnchecked(ActiveContext<C> active, ExtensionContext extension) {
        ((TestcontainersProvider<A, C>) active.provider).beforeStop((A) active.annotation, active.context, extension);
    }

    private static <A extends Annotation, C> void beforeEachUnchecked(ActiveContext<C> active, ExtensionContext extension) {
        ((TestcontainersProvider<A, C>) active.provider).beforeEach((A) active.annotation, active.context, extension);
    }

    private static <A extends Annotation, C> void afterEachUnchecked(ActiveContext<C> active, ExtensionContext extension) {
        ((TestcontainersProvider<A, C>) active.provider).afterEach((A) active.annotation, active.context, extension);
    }

    private static void stopUnchecked(ActiveContext<?> active) {
        active.context.stop();
    }

    private static List<TestcontainersProvider<?, ?>> loadProviders() {
        List<TestcontainersProvider<?, ?>> loaded = new ArrayList<>();
        for (TestcontainersProvider<?, ?> provider : ServiceLoader.load(TestcontainersProvider.class)) {
            loaded.add(provider);
        }
        return loaded;
    }
}
