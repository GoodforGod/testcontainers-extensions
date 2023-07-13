package io.goodforgod.testcontainers.extensions.jdbc;

import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
record ContainerMetadata(String image, ContainerMode runMode, Migration migration) {}
