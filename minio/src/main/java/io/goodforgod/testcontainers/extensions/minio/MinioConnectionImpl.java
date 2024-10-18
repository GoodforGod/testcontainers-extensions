package io.goodforgod.testcontainers.extensions.minio;

import io.minio.MinioClient;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
class MinioConnectionImpl implements MinioConnection {

    private static final class ParamsImpl implements Params {

        private final String host;
        private final int port;
        private final String accessKey;
        private final String secretKey;

        private ParamsImpl(String host, int port, String accessKey, String secretKey) {
            this.host = host;
            this.port = port;
            this.accessKey = Objects.requireNonNull(accessKey);
            this.secretKey = Objects.requireNonNull(secretKey);
        }

        @Override
        public @NotNull URI uri() {
            return URI.create(String.format("http://%s:%s", host, port));
        }

        @Override
        public String host() {
            return host;
        }

        @Override
        public int port() {
            return port;
        }

        @Override
        public String accessKey() {
            return accessKey;
        }

        @Override
        public String secretKey() {
            return secretKey;
        }

        @Override
        public String toString() {
            return uri().toString();
        }
    }

    private final Params params;
    private final Params network;

    private final MinioClient client;

    MinioConnectionImpl(Params params, Params network) {
        this.params = params;
        this.network = network;
        try {
            this.client = MinioClient.builder()
                    .endpoint(params.uri().toURL())
                    .credentials(params.accessKey(), params.secretKey())
                    .build();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static MinioConnection forContainer(String host,
                                        int port,
                                        String accessKey,
                                        String secretKey,
                                        String hostInNetwork,
                                        int portInNetwork) {
        var params = new ParamsImpl(host, port, accessKey, secretKey);
        final Params network;
        if (hostInNetwork == null) {
            network = null;
        } else {
            network = new ParamsImpl(hostInNetwork, portInNetwork, accessKey, secretKey);
        }

        return new MinioConnectionImpl(params, network);
    }

    static MinioConnection forExternal(String host, int port, String accessKey, String secretKey) {
        var params = new ParamsImpl(host, port, accessKey, secretKey);
        return new MinioConnectionImpl(params, null);
    }

    @Override
    public @NotNull Params params() {
        return params;
    }

    @Override
    public @NotNull Optional<Params> paramsInNetwork() {
        return Optional.ofNullable(network);
    }

    @Override
    public @NotNull MinioClient client() {
        return client;
    }

    void stop() {
        try {
            client.close();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MinioConnectionImpl that = (MinioConnectionImpl) o;
        return Objects.equals(params, that.params) && Objects.equals(network, that.network);
    }

    @Override
    public int hashCode() {
        return Objects.hash(params, network);
    }

    @Override
    public String toString() {
        return params().toString();
    }
}
