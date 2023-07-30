package io.goodforgod.testcontainers.extensions.cassandra;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Internal
public final class TestcontainersCassandraExtensionListener implements TestExecutionListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        for (ExtensionContainer container : TestcontainersCassandraExtension.getSharedContainers()) {
            logger.debug("Stopping in mode '{}' Cassandra Container: {}", ContainerMode.PER_RUN, container);
            container.stop();
            logger.debug("Stopped successfully in mode '{}' Cassandra Container: {}", ContainerMode.PER_RUN, container);
        }
    }
}
