package io.goodforgod.testcontainers.extensions.example;

import java.net.URI;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RedisConnectionImpl implements RedisConnection {

    private static final class ParamsImpl implements Params {

        private final String host;
        private final int port;
        private final String username;
        private final String password;
        private final int database;

        ParamsImpl(String host, int port, String username, String password, int database) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.database = database;
        }

        @Override
        public @NotNull URI uri() {
            return (username() == null && password() == null)
                    ? URI.create(String.format("redis://%s:%s/%s", host(), port(), database()))
                    : URI.create(String.format("redis://%s:%s@%s:%s/%s", username(), password(), host(), port(), database()));
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
        public String username() {
            return username;
        }

        @Override
        public String password() {
            return password;
        }

        @Override
        public int database() {
            return database;
        }

        @Override
        public String toString() {
            return uri().toString();
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(RedisConnection.class);

    private final Params params;
    private final Params network;

    RedisConnectionImpl(Params params, Params network) {
        this.params = params;
        this.network = network;
    }

    static RedisConnection forContainer(String host,
                                        int port,
                                        String hostInNetwork,
                                        Integer portInNetwork,
                                        int database,
                                        String username,
                                        String password) {
        var params = new ParamsImpl(host, port, username, password, database);
        final Params network;
        if (hostInNetwork == null) {
            network = null;
        } else {
            network = new ParamsImpl(hostInNetwork, portInNetwork, username, password, database);
        }

        return new RedisConnectionImpl(params, network);
    }

    static RedisConnection forExternal(String host,
                                       int port,
                                       int database,
                                       String username,
                                       String password) {
        var params = new ParamsImpl(host, port, username, password, database);
        return new RedisConnectionImpl(params, null);
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
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RedisConnectionImpl that = (RedisConnectionImpl) o;
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
