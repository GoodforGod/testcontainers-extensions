package io.goodforgod.testcontainers.extensions.redis;

import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
interface ExtensionContainer {

    void stop();
}
