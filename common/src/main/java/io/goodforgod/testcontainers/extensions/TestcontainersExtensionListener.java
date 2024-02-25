package io.goodforgod.testcontainers.extensions;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Internal
public final class TestcontainersExtensionListener implements TestExecutionListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        for (var imageToContainers : AbstractTestcontainersExtension.CLASS_TO_SHARED_CONTAINERS.entrySet()) {
            for (var imageToContainer : imageToContainers.getValue().entrySet()) {
                logger.debug("Stopping in mode '{}' container: {}", ContainerMode.PER_RUN, imageToContainer.getValue());
                imageToContainer.getValue().stop();
                logger.info("Stopped in mode '{}' container: {}", ContainerMode.PER_RUN, imageToContainer.getValue());
            }
        }
    }
}
