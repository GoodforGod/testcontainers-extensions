package io.goodforgod.testcontainers.extensions.rabbitmq;

import io.goodforgod.testcontainers.extensions.AbstractContainerMetadata;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class RabbitMQMetadata extends AbstractContainerMetadata {

    private final RabbitMQConnection.TopologySpec topology;
    private final Topology.Mode reset;

    RabbitMQMetadata(boolean network,
                     String alias,
                     String image,
                     ContainerMode runMode,
                     RabbitMQConnection.TopologySpec topology,
                     Topology.Mode reset) {
        super(network, alias, image, runMode);
        this.topology = topology;
        this.reset = reset;
    }

    RabbitMQConnection.TopologySpec topology() {
        return topology;
    }

    Topology.Mode reset() {
        return reset;
    }
}
