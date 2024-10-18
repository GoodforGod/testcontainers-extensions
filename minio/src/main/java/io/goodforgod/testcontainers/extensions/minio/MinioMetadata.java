package io.goodforgod.testcontainers.extensions.minio;

import io.goodforgod.testcontainers.extensions.AbstractContainerMetadata;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class MinioMetadata extends AbstractContainerMetadata {

    private final Bucket bucket;

    MinioMetadata(boolean network, String alias, String image, ContainerMode runMode, Bucket bucket) {
        super(network, alias, image, runMode);
        this.bucket = bucket;
    }

    Bucket bucket() {
        return bucket;
    }
}
