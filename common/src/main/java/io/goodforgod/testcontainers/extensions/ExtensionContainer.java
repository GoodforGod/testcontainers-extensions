package io.goodforgod.testcontainers.extensions;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.testcontainers.containers.GenericContainer;

@Internal
public interface ExtensionContainer<Container extends GenericContainer<?>, Connection> {

    Container container();

    Connection connection();

    void stop();
}
