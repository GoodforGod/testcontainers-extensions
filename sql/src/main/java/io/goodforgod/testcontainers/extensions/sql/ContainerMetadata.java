package io.goodforgod.testcontainers.extensions.sql;

record ContainerMetadata(String image, ContainerMode runMode, Migration migration) { }
