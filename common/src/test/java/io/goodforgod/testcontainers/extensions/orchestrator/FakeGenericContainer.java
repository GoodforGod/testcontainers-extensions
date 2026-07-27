package io.goodforgod.testcontainers.extensions.orchestrator;

import org.testcontainers.containers.GenericContainer;

public final class FakeGenericContainer extends GenericContainer<FakeGenericContainer> {

    private final String image;

    public FakeGenericContainer(String image) {
        super(image);
        this.image = image;
    }

    @Override
    public String getDockerImageName() {
        return image;
    }
}
