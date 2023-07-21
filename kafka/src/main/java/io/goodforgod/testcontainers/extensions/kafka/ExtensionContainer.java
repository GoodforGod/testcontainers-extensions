package io.goodforgod.testcontainers.extensions.kafka;

import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
interface ExtensionContainer {

    void stop();
}
