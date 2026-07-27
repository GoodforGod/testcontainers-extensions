package io.goodforgod.testcontainers.extensions.arangodb;

import io.goodforgod.testcontainers.extensions.AbstractContainerMetadata;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class ArangoMetadata extends AbstractContainerMetadata {

    private final String password;

    ArangoMetadata(boolean networkShared,
                   String networkAlias,
                   String image,
                   ContainerMode runMode,
                   String password) {
        super(networkShared, networkAlias, image, runMode);
        this.password = password;
    }

    String password() {
        return password;
    }
}
