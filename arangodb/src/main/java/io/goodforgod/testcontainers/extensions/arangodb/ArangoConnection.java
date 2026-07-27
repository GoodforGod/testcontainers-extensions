package io.goodforgod.testcontainers.extensions.arangodb;

import com.arangodb.ArangoDB;
import io.testcontainers.arangodb.containers.ArangoContainer;
import java.net.URI;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Describes active ArangoDB connection of currently running {@link ArangoContainer}.
 */
public interface ArangoConnection {

    /**
     * ArangoDB connection parameters.
     */
    interface Params {

        @NotNull
        URI uri();

        @NotNull
        String host();

        int port();

        @NotNull
        String username();

        String password();
    }

    /**
     * @return connection parameters to container
     */
    @NotNull
    Params params();

    /**
     * @return connection parameters inside docker network, can be useful when one container require
     *             params to connect to ArangoDB container inside docker network
     */
    @NotNull
    Optional<Params> paramsInNetwork();

    /**
     * @return ArangoDB client (DO NOT CLOSE)
     */
    @NotNull
    ArangoDB client();

    static ArangoConnection forContainer(ArangoContainer container) {
        if (!container.isRunning()) {
            throw new IllegalStateException(container.getClass().getSimpleName() + " container is not running");
        }

        var params = new ArangoConnectionImpl.ParamsImpl(container.getHost(), container.getPort(), container.getUser(),
                container.getPassword());
        final String alias = container.getNetworkAliases().get(container.getNetworkAliases().size() - 1);
        var network = new ArangoConnectionImpl.ParamsImpl(alias, ArangoContainer.PORT, container.getUser(),
                container.getPassword());
        return new ArangoConnectionImpl(params, network);
    }

    static ArangoConnection forParams(String host, int port, String username, String password) {
        var params = new ArangoConnectionImpl.ParamsImpl(host, port, username, password);
        return new ArangoConnectionImpl(params, null);
    }
}
