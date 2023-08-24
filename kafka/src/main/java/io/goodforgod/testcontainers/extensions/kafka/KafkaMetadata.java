package io.goodforgod.testcontainers.extensions.kafka;

import io.goodforgod.testcontainers.extensions.AbstractContainerMetadata;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.util.Set;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
final class KafkaMetadata extends AbstractContainerMetadata {

    private final Set<String> topics;
    private final Topics.Mode reset;

    KafkaMetadata(boolean network, String alias, String image, ContainerMode runMode, Set<String> topics, Topics.Mode reset) {
        super(network, alias, image, runMode);
        this.topics = topics;
        this.reset = reset;
    }

    @Override
    protected @NotNull String networkAliasDefault() {
        return "kafka-" + System.currentTimeMillis();
    }

    Set<String> topics() {
        return topics;
    }

    Topics.Mode reset() {
        return reset;
    }
}
