package io.goodforgod.testcontainers.extensions;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.testcontainers.containers.GenericContainer;

@Internal
final class ExtensionContainerImpl<Container extends GenericContainer<?>, Connection> implements
        ExtensionContainer<Container, Connection> {

    private final Container container;
    private final Connection connection;

    ExtensionContainerImpl(Container container, Connection connection) {
        this.container = container;
        this.connection = connection;
    }

    @Override
    public Container container() {
        return container;
    }

    @Override
    public Connection connection() {
        return connection;
    }

    @Override
    public void stop() {
        container.stop();
    }
}
