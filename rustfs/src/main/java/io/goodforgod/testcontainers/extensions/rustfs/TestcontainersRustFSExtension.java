package io.goodforgod.testcontainers.extensions.rustfs;

import io.goodforgod.testcontainers.extensions.AbstractTestcontainersExtension;
import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@Internal
class TestcontainersRustFSExtension extends
        AbstractTestcontainersExtension<RustFSConnection, RustFSContainer, RustFSMetadata> {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestcontainersRustFSExtension.class);

    protected Class<RustFSContainer> getContainerType() {
        return RustFSContainer.class;
    }

    protected Class<? extends Annotation> getContainerAnnotation() {
        return ContainerRustFS.class;
    }

    protected Class<? extends Annotation> getConnectionAnnotation() {
        return ConnectionRustFS.class;
    }

    @Override
    protected Class<RustFSConnection> getConnectionType() {
        return RustFSConnection.class;
    }

    @Override
    protected ExtensionContext.Namespace getNamespace() {
        return NAMESPACE;
    }

    @Override
    protected RustFSContainer createContainerDefault(RustFSMetadata metadata) {
        var image = DockerImageName.parse(metadata.image())
                .asCompatibleSubstituteFor(DockerImageName.parse("rustfs/rustfs"));

        final RustFSContainer container = new RustFSContainer(image);
        final String alias = Optional.ofNullable(metadata.networkAlias())
                .orElseGet(() -> "rustfs-" + System.currentTimeMillis());
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(RustFSContainer.class))
                .withMdc("image", image.asCanonicalNameString())
                .withMdc("alias", alias));
        container.withStartupTimeout(Duration.ofMinutes(2));
        container.setNetworkAliases(new ArrayList<>(List.of(alias)));
        if (metadata.networkShared()) {
            container.withNetwork(Network.SHARED);
        }

        return container;
    }

    @Override
    protected ContainerContext<RustFSConnection> createContainerContext(RustFSContainer container) {
        return new RustFSContext(container);
    }

    @NotNull
    protected Optional<RustFSMetadata> findMetadata(@NotNull ExtensionContext context) {
        return findAnnotation(TestcontainersRustFS.class, context)
                .map(a -> new RustFSMetadata(a.network().shared(), a.network().alias(), a.image(), a.mode(), a.bucket()));
    }

    private void tryCreateIfNotExist(RustFSMetadata metadata, RustFSConnection connection) {
        for (String bucket : metadata.bucket().value()) {
            try {
                boolean exist = connection.client().bucketExists(BucketExistsArgs.builder()
                        .bucket(bucket)
                        .build());

                if (!exist) {
                    connection.client().makeBucket(MakeBucketArgs.builder()
                            .bucket(bucket)
                            .build());
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void tryDropIfExist(RustFSMetadata metadata, RustFSConnection connection) {
        for (String bucket : metadata.bucket().value()) {
            try {
                boolean exist = connection.client().bucketExists(BucketExistsArgs.builder()
                        .bucket(bucket)
                        .build());

                if (exist) {
                    Iterable<Result<Item>> objects = connection.client().listObjects(ListObjectsArgs.builder()
                            .bucket(bucket)
                            .build());

                    Set<DeleteObject> deleteObjects = new LinkedHashSet<>();
                    for (Result<Item> rs : objects) {
                        Item item = rs.get();
                        deleteObjects.add(new DeleteObject(item.objectName()));
                    }

                    Iterable<Result<DeleteError>> results = connection.client().removeObjects(RemoveObjectsArgs.builder()
                            .bucket(bucket)
                            .objects(deleteObjects)
                            .build());

                    final List<DeleteError> errors = new ArrayList<>();
                    for (Result<DeleteError> result : results) {
                        DeleteError deleteError = result.get();
                        errors.add(deleteError);
                    }

                    if (!errors.isEmpty()) {
                        logger.warn("Failed to delete objects due to: {}", errors.stream()
                                .map(e -> "Bucket: " + e.bucketName() + ", Object: " + e.objectName() + ", errorMessage: "
                                        + e.message())
                                .collect(Collectors.joining(", ", "[", "]")));
                    }

                    connection.client().removeBucket(RemoveBucketArgs.builder()
                            .bucket(bucket)
                            .skipValidation(true)
                            .build());
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        super.beforeAll(context);

        var metadata = getMetadata(context);
        if (metadata.bucket().create() == Bucket.Mode.PER_CLASS) {
            var storage = getStorage(context);
            var connectionCurrent = getContainerContext(context).connection();
            tryCreateIfNotExist(metadata, connectionCurrent);
            storage.put(Bucket.class, metadata.bucket().create());
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var metadata = getMetadata(context);
        if (metadata.runMode() == ContainerMode.PER_METHOD && metadata.bucket().create() == Bucket.Mode.PER_CLASS) {
            throw new ExtensionConfigurationException(String.format(
                    "@%s can't apply migration in Bucket.Mode.PER_CLASS mode when ContainerMode.PER_METHOD is used",
                    getContainerAnnotation().getSimpleName()));
        }

        super.beforeEach(context);

        if (metadata.bucket().create() == Bucket.Mode.PER_METHOD) {
            var connectionCurrent = getContainerContext(context).connection();
            tryCreateIfNotExist(metadata, connectionCurrent);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var metadata = getMetadata(context);
        var storage = getStorage(context);
        storage.remove(Bucket.class);
        if (metadata.bucket().drop() == Bucket.Mode.PER_METHOD) {
            if (metadata.runMode() != ContainerMode.PER_METHOD) {
                var connectionCurrent = getContainerContext(context).connection();
                tryDropIfExist(metadata, connectionCurrent);
            }
        }

        super.afterEach(context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        var metadata = getMetadata(context);
        var connectionCurrent = getContainerContext(context).connection();
        if (metadata.bucket().drop() == Bucket.Mode.PER_CLASS) {
            if (metadata.runMode() == ContainerMode.PER_RUN) {
                tryDropIfExist(metadata, connectionCurrent);
            }
        }

        super.afterAll(context);
    }
}
