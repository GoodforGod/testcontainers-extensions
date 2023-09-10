package io.goodforgod.testcontainers.extensions.mockserver;

import java.net.URI;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.mockserver.client.MockServerClient;

/**
 * Describes active Mockserver connection of currently running
 * {@link MockServerContainerExtra}
 */
public interface MockserverConnection {

    /**
     * Mockserver connection parameters
     */
    interface Params {

        @NotNull
        URI uri();

        @NotNull
        String host();

        int port();
    }

    /**
     * @return connection parameters to container
     */
    @NotNull
    Params params();

    /**
     * @return connection parameters inside docker network, can be useful when one container require
     *             params to connect to Mockserver container inside docker network
     */
    @NotNull
    Optional<Params> paramsInNetwork();

    /**
     * @return MockServer Client
     */
    @NotNull
    MockServerClient client();
}
