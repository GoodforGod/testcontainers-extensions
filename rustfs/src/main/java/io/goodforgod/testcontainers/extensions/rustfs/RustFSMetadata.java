package io.goodforgod.testcontainers.extensions.rustfs;

import io.goodforgod.testcontainers.extensions.AbstractContainerMetadata;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class RustFSMetadata extends AbstractContainerMetadata {

    private final Bucket bucket;

    RustFSMetadata(boolean network, String alias, String image, ContainerMode runMode, Bucket bucket) {
        super(network, alias, image, runMode);
        this.bucket = bucket;
    }

    Bucket bucket() {
        return bucket;
    }
}
