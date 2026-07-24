package io.goodforgod.testcontainers.extensions.orchestrator;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;

public final class FakeContainerContext implements ContainerContext<FakeConnection> {

    private final String service;
    private final String image;
    private final GenericContainer<?> container;
    private volatile boolean running;

    public FakeContainerContext(String service, String image, GenericContainer<?> container) {
        this.service = service;
        this.image = image;
        this.container = container;
    }

    @Override
    public @NotNull FakeConnection connection() {
        if (!running) {
            throw new IllegalStateException(service + " container is not running");
        }

        return new FakeConnection(service,
                image,
                List.copyOf(container.getNetworkAliases()),
                container.getNetwork() != null,
                System.identityHashCode(container.getNetwork()),
                service);
    }

    @Override
    public void start() {
        long startedAt = System.nanoTime();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }

        running = true;
        StartTimeline.started(service, startedAt, System.nanoTime());
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public String toString() {
        return service;
    }
}
