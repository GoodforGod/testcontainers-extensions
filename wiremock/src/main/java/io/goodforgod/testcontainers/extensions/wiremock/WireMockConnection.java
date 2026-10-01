package io.goodforgod.testcontainers.extensions.wiremock;

import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.URI;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Describes active WireMock connection of currently running
 * {@link org.wiremock.integrations.testcontainers.WireMockContainer}
 */
public interface WireMockConnection {

    /**
     * WireMock connection parameters
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
     *             params to connect to WireMock container inside docker network
     */
    @NotNull
    Optional<Params> paramsInNetwork();

    /**
     * @return WireMock admin client for stubbing and verification against the running container
     */
    @NotNull
    WireMock client();
}
