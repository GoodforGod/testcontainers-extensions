package io.goodforgod.testcontainers.extensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.testcontainers.containers.GenericContainer;

/**
 * Provider contract for the shared Testcontainers orchestrator.
 *
 * @param <A> annotation type that enables container support
 * @param <C> connection type exposed to tests
 */
public interface TestcontainersProvider<A extends Annotation, C> {

    @NotNull
    Class<A> annotationType();

    @NotNull
    Class<? extends Annotation> containerAnnotationType();

    @NotNull
    Class<? extends Annotation> connectionAnnotationType();

    @NotNull
    Class<C> connectionType();

    @NotNull
    ContainerMode mode(@NotNull A annotation);

    @NotNull
    String image(@NotNull A annotation);

    boolean networkShared(@NotNull A annotation);

    String networkAlias(@NotNull A annotation);

    default Set<Class<? extends Annotation>> dependencies(@NotNull A annotation) {
        return Collections.emptySet();
    }

    default Isolation.Mode isolation(@NotNull A annotation) {
        try {
            Method isolation = annotation.annotationType().getMethod("isolation");
            Object value = isolation.invoke(annotation);
            return (value instanceof Isolation configured)
                    ? configured.value()
                    : Isolation.Mode.DISABLED;
        } catch (NoSuchMethodException e) {
            return Isolation.Mode.DISABLED;
        } catch (Exception e) {
            throw new IllegalStateException("@%s isolation() can't read".formatted(annotationType().getSimpleName()), e);
        }
    }

    default String isolationPrefix(@NotNull A annotation) {
        return annotationType().getSimpleName().toLowerCase(Locale.ROOT);
    }

    @NotNull
    GenericContainer<?> createContainer(@NotNull A annotation);

    @NotNull
    ContainerContext<C> createContext(@NotNull GenericContainer<?> container);

    default @NotNull C createIsolatedConnection(@NotNull A annotation,
                                                @NotNull ContainerContext<C> context,
                                                @NotNull ExtensionContext extension,
                                                @NotNull String namespace) {
        throw new UnsupportedOperationException("@%s doesn't support Isolation.Mode.PER_METHOD"
                .formatted(annotationType().getSimpleName()));
    }

    default void closeIsolatedConnection(@NotNull A annotation,
                                         @NotNull C connection,
                                         @NotNull ExtensionContext extension) {
        if (connection instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                throw new IllegalStateException("Isolated connection can't close", e);
            }
        }
    }

    default void afterStart(@NotNull A annotation, @NotNull ContainerContext<C> context, @NotNull ExtensionContext extension) {}

    default void beforeEach(@NotNull A annotation, @NotNull ContainerContext<C> context, @NotNull ExtensionContext extension) {}

    default void afterEach(@NotNull A annotation, @NotNull ContainerContext<C> context, @NotNull ExtensionContext extension) {}

    default void beforeStop(@NotNull A annotation, @NotNull ContainerContext<C> context, @NotNull ExtensionContext extension) {}

    default Object resolveParameter(@NotNull C connection, @NotNull ParameterContext parameter) {
        return connection;
    }

    default Object resolveParameter(@NotNull ContainerContext<C> context, @NotNull ParameterContext parameter) {
        return resolveParameter(context.connection(), parameter);
    }

    default void injectField(@NotNull C connection, @NotNull Field field, @NotNull Object instance) {
        try {
            field.setAccessible(true);
            field.set(instance, connection);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Field '%s' can't set connection".formatted(field.getName()), e);
        }
    }

    default void injectField(@NotNull ContainerContext<C> context, @NotNull Field field, @NotNull Object instance) {
        injectField(context.connection(), field, instance);
    }
}
