package io.goodforgod.testcontainers.extensions.microcks;

import io.github.microcks.testcontainers.MicrocksContainer;
import io.github.microcks.testcontainers.model.ServiceRef;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
class MicrocksConnectionImpl implements MicrocksConnection {

    private static final class ParamsImpl implements Params {

        private final String host;
        private final int port;
        private final int grpcPort;

        ParamsImpl(String host, int port, int grpcPort) {
            this.host = host;
            this.port = port;
            this.grpcPort = grpcPort;
        }

        @Override
        public @NotNull URI uri() {
            return URI.create(String.format("http://%s:%d", host, port));
        }

        @Override
        public @NotNull String host() {
            return host;
        }

        @Override
        public int port() {
            return port;
        }

        @Override
        public int grpcPort() {
            return grpcPort;
        }

        @Override
        public @NotNull URI restMockEndpoint(@NotNull String service, @NotNull String version) {
            return URI.create(String.format("%s/rest/%s/%s", uri(), service, version));
        }

        @Override
        public @NotNull URI soapMockEndpoint(@NotNull String service, @NotNull String version) {
            return URI.create(String.format("%s/soap/%s/%s", uri(), service, version));
        }

        @Override
        public @NotNull URI graphQLMockEndpoint(@NotNull String service, @NotNull String version) {
            return URI.create(String.format("%s/graphql/%s/%s", uri(), service, version));
        }

        @Override
        public @NotNull URI grpcMockEndpoint() {
            return URI.create(String.format("grpc://%s:%d", host, grpcPort));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ParamsImpl that = (ParamsImpl) o;
            return port == that.port && grpcPort == that.grpcPort && Objects.equals(host, that.host);
        }

        @Override
        public int hashCode() {
            return Objects.hash(host, port, grpcPort);
        }

        @Override
        public String toString() {
            return uri().toString();
        }
    }

    private final Params params;
    private final Params network;

    private MicrocksConnectionImpl(Params params, Params network) {
        this.params = params;
        this.network = network;
    }

    static MicrocksConnection forContainer(String host,
                                           int port,
                                           int grpcPort,
                                           String hostInNetwork,
                                           int portInNetwork,
                                           int grpcPortInNetwork) {
        var params = new ParamsImpl(host, port, grpcPort);
        final Params network;
        if (hostInNetwork == null) {
            network = null;
        } else {
            network = new ParamsImpl(hostInNetwork, portInNetwork, grpcPortInNetwork);
        }

        return new MicrocksConnectionImpl(params, network);
    }

    static MicrocksConnection forExternal(String host, int port, int grpcPort) {
        var params = new ParamsImpl(host, port, grpcPort);
        return new MicrocksConnectionImpl(params, null);
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
    public void importAsMainArtifact(@NotNull File artifact) {
        try {
            MicrocksContainer.importArtifact(params.uri().toString(), artifact, true);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void importAsSecondaryArtifact(@NotNull File artifact) {
        try {
            MicrocksContainer.importArtifact(params.uri().toString(), artifact, false);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public @NotNull List<ServiceRef> getServices() {
        try {
            return MicrocksContainer.getServices(params.uri().toString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    void stop() {
        // Microcks connection holds no client resources that require closing
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MicrocksConnectionImpl that = (MicrocksConnectionImpl) o;
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
