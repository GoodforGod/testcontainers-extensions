package io.goodforgod.testcontainers.extensions;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.orchestrator.FakeConnection;
import io.goodforgod.testcontainers.extensions.orchestrator.redis.ConnectionRedis;
import io.goodforgod.testcontainers.extensions.orchestrator.redis.TestcontainersRedis;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestcontainersRedis(mode = ContainerMode.PER_CLASS)
class OrchestratorIsolationCompatibilityTests {

    @ConnectionRedis
    private FakeConnection redis;

    @Test
    void disabledIsolationKeepsPerClassFieldInjection() {
        assertNotNull(redis);
        assertEquals("redis", redis.namespace());
    }

    @Test
    void perMethodIsolationRejectsPerClassLifecycle() throws Exception {
        Method validate = TestcontainersOrchestratorExtension.class
                .getDeclaredMethod("validateIsolationLifecycle", ExtensionContext.class);
        validate.setAccessible(true);

        var thrown = assertThrows(Exception.class, () -> validate.invoke(new TestcontainersOrchestratorExtension(),
                context(PerClassLifecycleWithIsolation.class)));
        assertInstanceOf(ExtensionConfigurationException.class, thrown.getCause());
        assertTrue(thrown.getCause().getMessage().contains("Isolation.Mode.PER_METHOD"));
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Disabled("Selected through direct extension validation test")
    @TestcontainersRedis(mode = ContainerMode.PER_CLASS, isolation = @Isolation(Isolation.Mode.PER_METHOD))
    static class PerClassLifecycleWithIsolation {

        @ConnectionRedis
        private FakeConnection redis;

        @Test
        void failsBeforeInjection() {
            assertNotNull(redis);
        }
    }

    private static ExtensionContext context(Class<?> testClass) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getTestClass" -> java.util.Optional.of(testClass);
            case "getRequiredTestClass" -> testClass;
            case "getTestInstanceLifecycle" -> java.util.Optional.of(TestInstance.Lifecycle.PER_CLASS);
            case "getParent" -> java.util.Optional.empty();
            default -> throw new UnsupportedOperationException(method.getName());
        };
        return (ExtensionContext) Proxy.newProxyInstance(ExtensionContext.class.getClassLoader(),
                new Class<?>[] { ExtensionContext.class },
                handler);
    }
}
