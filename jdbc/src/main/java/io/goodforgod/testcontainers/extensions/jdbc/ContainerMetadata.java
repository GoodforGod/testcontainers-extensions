package io.goodforgod.testcontainers.extensions.jdbc;

record ContainerMetadata(String image, ContainerMode runMode, Migration migration) {}
