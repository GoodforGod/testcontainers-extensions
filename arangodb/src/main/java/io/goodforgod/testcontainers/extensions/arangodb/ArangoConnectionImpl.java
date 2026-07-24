package io.goodforgod.testcontainers.extensions.arangodb;

import com.arangodb.ArangoDB;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
class ArangoConnectionImpl implements ArangoConnection {

    static final class ParamsImpl implements Params {

        private final String host;
        private final int port;
        private final String username;
        private final String password;

        ParamsImpl(String host, int port, String username, String password) {
            this.host = host;
            this.port = port;
            this.username = Objects.requireNonNull(username);
            this.password = password;
        }

        @Override
        public @NotNull URI uri() {
            return URI.create(String.format("http://%s:%s", host(), port()));
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
        public @NotNull String username() {
            return username;
        }

        @Override
        public String password() {
            return password;
        }

        @Override
        public String toString() {
            return uri().toString();
        }
    }

    private final Params params;
    private final Params network;
    private final ArangoDB client;

    ArangoConnectionImpl(Params params, Params network) {
        this.params = params;
        this.network = network;

        try {
            var builder = new ArangoDB.Builder()
                    .host(params.host(), params.port())
                    .user(params.username());
            if (params.password() != null && !params.password().isBlank()) {
                builder.password(params.password());
            }

            this.client = builder.build();
        } catch (Exception e) {
            throw new ArangoConnectionException(e);
        }
    }

    static ArangoConnection forContainer(String host,
                                         int port,
                                         String hostInNetwork,
                                         Integer portInNetwork,
                                         String username,
                                         String password) {
        var params = new ParamsImpl(host, port, username, password);
        final Params network;
        if (hostInNetwork == null) {
            network = null;
        } else {
            network = new ParamsImpl(hostInNetwork, portInNetwork, username, password);
        }

        return new ArangoConnectionImpl(params, network);
    }

    static ArangoConnection forExternal(String host, int port, String username, String password) {
        var params = new ParamsImpl(host, port, username, password);
        return new ArangoConnectionImpl(params, null);
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
    public @NotNull ArangoDB client() {
        return client;
    }

    void stop() {
        client.shutdown();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ArangoConnectionImpl that = (ArangoConnectionImpl) o;
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
