package io.goodforgod.testcontainers.extensions.cassandra;

import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
interface ExtensionContainer {

    void stop();
}
