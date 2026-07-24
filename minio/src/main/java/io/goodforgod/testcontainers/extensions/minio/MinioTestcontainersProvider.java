package io.goodforgod.testcontainers.extensions.minio;

import io.goodforgod.testcontainers.extensions.ContainerContext;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.TestcontainersProvider;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;

@Internal
public final class MinioTestcontainersProvider implements TestcontainersProvider<TestcontainersMinio, MinioConnection> {

    private static final Logger logger = LoggerFactory.getLogger(MinioTestcontainersProvider.class);

    private final TestcontainersMinioExtension delegate = new TestcontainersMinioExtension();

    @Override
    public @NotNull Class<TestcontainersMinio> annotationType() {
        return TestcontainersMinio.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> containerAnnotationType() {
        return ContainerMinio.class;
    }

    @Override
    public @NotNull Class<? extends Annotation> connectionAnnotationType() {
        return ConnectionMinio.class;
    }

    @Override
    public @NotNull Class<MinioConnection> connectionType() {
        return MinioConnection.class;
    }

    @Override
    public @NotNull ContainerMode mode(@NotNull TestcontainersMinio annotation) {
        return annotation.mode();
    }

    @Override
    public @NotNull String image(@NotNull TestcontainersMinio annotation) {
        return annotation.image();
    }

    @Override
    public boolean networkShared(@NotNull TestcontainersMinio annotation) {
        return annotation.network().shared();
    }

    @Override
    public String networkAlias(@NotNull TestcontainersMinio annotation) {
        return annotation.network().alias();
    }

    @Override
    public @NotNull GenericContainer<?> createContainer(@NotNull TestcontainersMinio annotation) {
        validate(annotation);
        var metadata = metadata(annotation);
        return delegate.createContainerDefault(metadata);
    }

    @Override
    public @NotNull ContainerContext<MinioConnection> createContext(@NotNull GenericContainer<?> container) {
        return delegate.createContainerContext((MinIOContainer) container);
    }

    @Override
    public void afterStart(@NotNull TestcontainersMinio annotation,
                           @NotNull ContainerContext<MinioConnection> context,
                           @NotNull ExtensionContext extension) {
        if (annotation.bucket().create() == Bucket.Mode.PER_CLASS) {
            createIfNotExist(annotation, context.connection());
        }
    }

    @Override
    public void beforeEach(@NotNull TestcontainersMinio annotation,
                           @NotNull ContainerContext<MinioConnection> context,
                           @NotNull ExtensionContext extension) {
        if (annotation.bucket().create() == Bucket.Mode.PER_METHOD) {
            createIfNotExist(annotation, context.connection());
        }
    }

    @Override
    public void afterEach(@NotNull TestcontainersMinio annotation,
                          @NotNull ContainerContext<MinioConnection> context,
                          @NotNull ExtensionContext extension) {
        if (annotation.bucket().drop() == Bucket.Mode.PER_METHOD && annotation.mode() != ContainerMode.PER_METHOD) {
            dropIfExist(annotation, context.connection());
        }
    }

    @Override
    public void beforeStop(@NotNull TestcontainersMinio annotation,
                           @NotNull ContainerContext<MinioConnection> context,
                           @NotNull ExtensionContext extension) {
        if (annotation.bucket().drop() == Bucket.Mode.PER_CLASS && annotation.mode() == ContainerMode.PER_RUN) {
            dropIfExist(annotation, context.connection());
        }
    }

    private static MinioMetadata metadata(TestcontainersMinio annotation) {
        return new MinioMetadata(annotation.network().shared(), annotation.network().alias(), annotation.image(),
                annotation.mode(), annotation.bucket());
    }

    private static void validate(TestcontainersMinio annotation) {
        if (annotation.mode() == ContainerMode.PER_METHOD && annotation.bucket().create() == Bucket.Mode.PER_CLASS) {
            throw new ExtensionConfigurationException(String.format(
                    "@%s can't apply migration in Bucket.Mode.PER_CLASS mode when ContainerMode.PER_METHOD is used",
                    ContainerMinio.class.getSimpleName()));
        }
    }

    private static void createIfNotExist(TestcontainersMinio annotation, MinioConnection connection) {
        for (String bucket : annotation.bucket().value()) {
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

    private static void dropIfExist(TestcontainersMinio annotation, MinioConnection connection) {
        for (String bucket : annotation.bucket().value()) {
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
                        deleteObjects.add(new DeleteObject(rs.get().objectName()));
                    }

                    Iterable<Result<DeleteError>> results = connection.client().removeObjects(RemoveObjectsArgs.builder()
                            .bucket(bucket)
                            .objects(deleteObjects)
                            .build());

                    List<DeleteError> errors = new ArrayList<>();
                    for (Result<DeleteError> result : results) {
                        errors.add(result.get());
                    }

                    if (!errors.isEmpty()) {
                        logger.warn("Failed to delete objects due to: {}", errors.stream()
                                .map(e -> "Bucket: " + e.bucketName() + ", Object: " + e.objectName()
                                        + ", errorMessage: " + e.message())
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
}
