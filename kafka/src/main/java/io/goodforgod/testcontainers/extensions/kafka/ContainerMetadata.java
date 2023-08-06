package io.goodforgod.testcontainers.extensions.kafka;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.util.List;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class ContainerMetadata {

    private final String image;
    private final ContainerMode runMode;
    private final List<String> topics;

    ContainerMetadata(String image, ContainerMode runMode, List<String> topics) {
        this.image = image;
        this.runMode = runMode;
        this.topics = topics;
    }

    String image() {
        return image;
    }

    ContainerMode runMode() {
        return runMode;
    }

    List<String> topics() {
        return topics;
    }
}
