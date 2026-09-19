package io.goodforgod.testcontainers.extensions.microcks;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

@TestcontainersMicrocks(mode = ContainerMode.PER_CLASS)
class MicrocksConnectionAssertsTests {

    @ConnectionMicrocks
    private MicrocksConnection connection;

    @Test
    void importsContractAndMocksRest() throws Exception {
        File contract = new File(getClass().getResource("/hello-openapi.yaml").toURI());
        connection.importAsMainArtifact(contract);

        assertFalse(connection.getServices().isEmpty());

        URI url = URI.create(connection.params().restMockEndpoint("HelloAPI", "1.0.0") + "/hello");
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(url).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("hello"));
    }
}
