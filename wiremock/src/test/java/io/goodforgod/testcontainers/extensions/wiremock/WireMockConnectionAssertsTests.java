package io.goodforgod.testcontainers.extensions.wiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

@TestcontainersWireMock(mode = ContainerMode.PER_CLASS)
class WireMockConnectionAssertsTests {

    @ConnectionWireMock
    private WireMockConnection connection;

    @Test
    void stubIsRegistered() {
        connection.client().register(get(urlEqualTo("/get"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("OK")));

        assertFalse(connection.client().allStubMappings().getMappings().isEmpty());
    }

    @Test
    void stubRespondsOverHttp() throws IOException, InterruptedException {
        connection.client().register(get(urlEqualTo("/hello"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("world")));

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(connection.params().uri() + "/hello")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("world", response.body());
    }
}
