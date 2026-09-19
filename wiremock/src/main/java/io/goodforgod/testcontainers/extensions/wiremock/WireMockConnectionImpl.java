package io.goodforgod.testcontainers.extensions.wiremock;

import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
class WireMockConnectionImpl implements WireMockConnection {

    private static final class ParamsImpl implements Params {

        private final String host;
        private final int port;

        ParamsImpl(String host, int port) {
            this.host = host;
            this.port = port;
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
        public String toString() {
            return uri().toString();
        }
    }

    private final Params params;
    private final Params network;

    private final WireMock client;

    WireMockConnectionImpl(Params params, Params network) {
        this.params = params;
        this.network = network;
        this.client = new WireMock(params.host(), params.port());
    }

    static WireMockConnection forContainer(String host, int port, String hostInNetwork, Integer portInNetwork) {
        var params = new ParamsImpl(host, port);
        final Params network;
        if (hostInNetwork == null) {
            network = null;
        } else {
            network = new ParamsImpl(hostInNetwork, portInNetwork);
        }

        return new WireMockConnectionImpl(params, network);
    }

    static WireMockConnection forExternal(String host, int port) {
        var params = new ParamsImpl(host, port);
        return new WireMockConnectionImpl(params, null);
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
    public @NotNull WireMock client() {
        return client;
    }

    void stop() {
        // WireMock admin client holds no resources that require closing
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        WireMockConnectionImpl that = (WireMockConnectionImpl) o;
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
