package io.goodforgod.testcontainers.extensions;

import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface ContainerContext<Connection> {

    Connection connection();

    void start();

    void stop();
}
