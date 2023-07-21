package io.goodforgod.testcontainers.extensions.kafka;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class ContainerMetadata {

    private final String image;
    private final ContainerMode runMode;

    ContainerMetadata(String image, ContainerMode runMode) {
        this.image = image;
        this.runMode = runMode;
    }

    String image() {
        return image;
    }

    ContainerMode runMode() {
        return runMode;
    }
}
