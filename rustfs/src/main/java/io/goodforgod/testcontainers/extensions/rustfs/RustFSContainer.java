package io.goodforgod.testcontainers.extensions.rustfs;

import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers container for <a href="https://hub.docker.com/r/rustfs/rustfs">RustFS</a>, a
 * high-performance S3 compatible object storage.
 * <p>
 * RustFS has no dedicated Testcontainers module, so this container mirrors the behavior MinioContainer
 */
public class RustFSContainer extends GenericContainer<RustFSContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("rustfs/rustfs");

    private static final int RUSTFS_S3_PORT = 9000;
    private static final int RUSTFS_CONSOLE_PORT = 9001;

    private static final String DEFAULT_ACCESS_KEY = "rustfsadmin";
    private static final String DEFAULT_SECRET_KEY = "rustfsadmin";

    private String accessKey = DEFAULT_ACCESS_KEY;
    private String secretKey = DEFAULT_SECRET_KEY;

    public RustFSContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public RustFSContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(RUSTFS_S3_PORT, RUSTFS_CONSOLE_PORT);
        setWaitStrategy(Wait.forHttp("/health/ready")
                .forPort(RUSTFS_S3_PORT)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(2)));
    }

    public RustFSContainer withAccessKey(String accessKey) {
        this.accessKey = accessKey;
        return this;
    }

    public RustFSContainer withSecretKey(String secretKey) {
        this.secretKey = secretKey;
        return this;
    }

    @Override
    protected void configure() {
        addEnv("RUSTFS_ACCESS_KEY", accessKey);
        addEnv("RUSTFS_SECRET_KEY", secretKey);
        addEnv("RUSTFS_ADDRESS", "0.0.0.0:" + RUSTFS_S3_PORT);
        addEnv("RUSTFS_CONSOLE_ADDRESS", "0.0.0.0:" + RUSTFS_CONSOLE_PORT);
        addEnv("RUSTFS_CONSOLE_ENABLE", "true");
    }

    public int getS3Port() {
        return RUSTFS_S3_PORT;
    }

    public String getS3URL() {
        return String.format("http://%s:%s", getHost(), getMappedPort(RUSTFS_S3_PORT));
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }
}
