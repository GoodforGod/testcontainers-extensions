package io.goodforgod.testcontainers.extensions.microcks;

import io.github.microcks.testcontainers.model.ServiceRef;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Describes active Microcks connection of currently running
 * {@link io.github.microcks.testcontainers.MicrocksContainer}.
 * <p>
 * Microcks turns API contracts (OpenAPI, AsyncAPI, gRPC/Protobuf, GraphQL, SOAP/WSDL, Postman) into
 * live mocks. Load contracts with {@link #importAsMainArtifact(File)} and call the mock endpoints
 * resolved from {@link #params()} (or {@link #paramsInNetwork()} for containers on the same
 * network)
 * from the code under test.
 */
public interface MicrocksConnection {

    /**
     * Microcks connection parameters. Mock endpoints are resolved relative to these parameters, so
     * {@link #params()} yields host-reachable endpoints while {@link #paramsInNetwork()} yields
     * endpoints reachable by other containers inside the same docker network.
     */
    interface Params {

        /**
         * @return base HTTP endpoint of the Microcks instance, e.g. {@code http://localhost:32769}
         */
        @NotNull
        URI uri();

        @NotNull
        String host();

        /**
         * @return HTTP port
         */
        int port();

        /**
         * @return gRPC port
         */
        int grpcPort();

        /**
         * @param service API/service name as declared in the imported contract
         * @param version API/service version as declared in the imported contract
         * @return REST mock endpoint for the given service and version
         */
        @NotNull
        URI restMockEndpoint(@NotNull String service, @NotNull String version);

        /**
         * @param service API/service name as declared in the imported contract
         * @param version API/service version as declared in the imported contract
         * @return SOAP mock endpoint for the given service and version
         */
        @NotNull
        URI soapMockEndpoint(@NotNull String service, @NotNull String version);

        /**
         * @param service API/service name as declared in the imported contract
         * @param version API/service version as declared in the imported contract
         * @return GraphQL mock endpoint for the given service and version
         */
        @NotNull
        URI graphQLMockEndpoint(@NotNull String service, @NotNull String version);

        /**
         * @return gRPC mock endpoint, e.g. {@code grpc://localhost:32770}
         */
        @NotNull
        URI grpcMockEndpoint();
    }

    /**
     * @return connection parameters to container, reachable from the host running the tests
     */
    @NotNull
    Params params();

    /**
     * @return connection parameters inside docker network, can be useful when one container require
     *             params to connect to Microcks container inside docker network
     */
    @NotNull
    Optional<Params> paramsInNetwork();

    /**
     * Imports an API contract as a main artifact, registering its mocks.
     *
     * @param artifact contract file (OpenAPI, AsyncAPI, gRPC/Protobuf, GraphQL, SOAP/WSDL, Postman)
     */
    void importAsMainArtifact(@NotNull File artifact);

    /**
     * Imports an API contract as a secondary artifact, enriching an already imported service.
     *
     * @param artifact contract file
     */
    void importAsSecondaryArtifact(@NotNull File artifact);

    /**
     * @return services currently registered in the Microcks instance
     */
    @NotNull
    List<ServiceRef> getServices();
}
