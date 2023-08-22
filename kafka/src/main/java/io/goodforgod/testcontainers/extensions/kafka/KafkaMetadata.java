package io.goodforgod.testcontainers.extensions.kafka;

import io.goodforgod.testcontainers.extensions.ContainerMetadata;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.util.Set;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
final class KafkaMetadata implements ContainerMetadata {

    private final boolean network;
    private final String image;
    private final ContainerMode runMode;
    private final Set<String> topics;
    private final Topics.Mode reset;

    KafkaMetadata(boolean network, String image, ContainerMode runMode, Set<String> topics, Topics.Mode reset) {
        this.network = network;
        this.image = image;
        this.runMode = runMode;
        this.topics = topics;
        this.reset = reset;
    }

    @Override
    public boolean useNetworkShared() {
        return network;
    }

    public @NotNull String image() {
        return image;
    }

    public @NotNull ContainerMode runMode() {
        return runMode;
    }

    Set<String> topics() {
        return topics;
    }

    Topics.Mode reset() {
        return reset;
    }
}
