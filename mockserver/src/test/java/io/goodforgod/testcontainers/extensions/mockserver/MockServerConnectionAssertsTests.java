package io.goodforgod.testcontainers.extensions.mockserver;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.Test;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

@TestcontainersMockServer(mode = ContainerMode.PER_CLASS)
class MockServerConnectionAssertsTests {

    @ConnectionMockServer
    private MockServerConnection connection;

    @Test
    void assertCountsAtLeastWhenEquals() {
        connection.client().when(HttpRequest.request()
                .withMethod("GET")
                .withPath("/get"))
                .respond(HttpResponse.response()
                        .withStatusCode(200)
                        .withBody("OK"));
    }

    @Test
    void assertCountsExactWhenZero() {
        connection.client().when(HttpRequest.request()
                .withMethod("GET")
                .withPath("/get"))
                .respond(HttpResponse.response()
                        .withStatusCode(200)
                        .withBody("OK"));
    }
}
