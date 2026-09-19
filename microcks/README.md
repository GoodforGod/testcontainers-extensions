# Testcontainers Extensions Microcks

[![Minimum required Java version](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Maven Central](https://maven-badges.herokuRpp.com/maven-central/io.goodforgod/testcontainers-extensions-microcks.svg)](https://central.sonatype.com/artifact/io.goodforgod/testcontainers-extensions-microcks)
[![GitHub Action](https://github.com/goodforgod/testcontainers-extensions/workflows/Release/badge.svg)](https://github.com/GoodforGod/testcontainers-extensions/actions?query=workflow%3A"CI+Master"++)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=coverage)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=ncloc)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)

Testcontainers [Microcks](https://microcks.io/) Extension with advanced testing capabilities.

Microcks turns API contracts into live mocks. Unlike request/response mocking tools, it generates mocks
directly from **API artifacts** — OpenAPI, AsyncAPI, gRPC/Protobuf, GraphQL, SOAP/WSDL and Postman
collections — so one integration covers REST, SOAP, GraphQL and gRPC mocking.

Features:
- Container easy run *per method*, *per class*, *per execution*.
- Container easy connection injection with asserts.

## Dependency :rocket:

**Gradle**
```groovy
testImplementation "io.goodforgod:testcontainers-extensions-microcks:0.16.0"
```

**Maven**
```xml
<dependency>
    <groupId>io.goodforgod</groupId>
    <artifactId>testcontainers-extensions-microcks</artifactId>
    <version>0.16.0</version>
    <scope>test</scope>
</dependency>
```

## Content
- [Usage](#usage)
- [Connection](#connection)
- [Annotation](#annotation)
  - [Manual Container](#manual-container)
  - [Connection](#annotation-connection)
  - [External Connection](#external-connection)

## Usage

Import an API contract as an artifact, then call the generated mock endpoint. Test with container start in
`PER_RUN` mode will look like:

```java
@TestcontainersMicrocks(mode = ContainerMode.PER_RUN)
class ExampleTests {

  @ConnectionMicrocks
  private MicrocksConnection connection;

  @Test
  void test() throws Exception {
    connection.importAsMainArtifact(new File("src/test/resources/hello-openapi.yaml"));

    // service name & version come from the contract's info.title / info.version
    URI endpoint = connection.params().restMockEndpoint("HelloAPI", "1.0.0");
    // call `endpoint + "/hello"` from the code under test; Microcks returns the contract's example
  }
}
```

## Connection

`MicrocksConnection` is an abstraction to import contracts, resolve mock endpoints and inspect registered services.
You can inject connection via `@ConnectionMicrocks` as field or method argument or manually create it from container or manual settings.

```java
class ExampleTests {

  @ConnectionMicrocks 
  private MicrocksConnection connection;
  
  @Test
  void test() {
    connection.importAsMainArtifact(new File("src/test/resources/hello-openapi.yaml"));

    URI base = connection.params().uri();
    URI rest = connection.params().restMockEndpoint("HelloAPI", "1.0.0");
    URI soap = connection.params().soapMockEndpoint("HelloSOAP", "1.0");
    URI graphql = connection.params().graphQLMockEndpoint("HelloGraphQL", "1.0");
    URI grpc = connection.params().grpcMockEndpoint();
  }
}
```

Available operations:

- `params()` / `paramsInNetwork()` - Microcks endpoint reachable from the host / from other containers on the same network.
- `importAsMainArtifact(File)` / `importAsSecondaryArtifact(File)` - load an API contract.
- `getServices()` - list registered services.

Mock endpoints are resolved on `Params`, so they are network-correct for whichever `params()` /
`paramsInNetwork()` you build from, and all return a `URI`:

- `params().uri()` - base HTTP endpoint.
- `params().restMockEndpoint(service, version)` / `soapMockEndpoint(...)` / `graphQLMockEndpoint(...)` / `grpcMockEndpoint()`.

## Annotation

`@TestcontainersMicrocks` - allow **automatically start container** with specified image in different modes without the need to configure it.

Available containers modes:

- `PER_RUN` - start container one time per *test execution*. (Containers must have same instance, e.g. compare by `==`)
- `PER_CLASS` - start new container each *test class*.
- `PER_METHOD` - start new container each *test method*.

Simple example on how to start container per class, **no need to configure** container:
```java
@TestcontainersMicrocks(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @Test
    void test(@ConnectionMicrocks MicrocksConnection connection) {
        assertNotNull(connection);
    }
}
```

**That's all** you need.

It is possible to customize image with annotation `image` parameter.

Image also can be provided from environment variable:
```java
@TestcontainersMicrocks(image = "${MY_IMAGE_ENV|quay.io/microcks/microcks-uber:1.15.0}")
class ExampleTests {

    @Test
    void test() {
        // test
    }
}
```

Image syntax:

- Image can have static value: `quay.io/microcks/microcks-uber:1.15.0`
- Image can be provided via environment variable using syntax: `${MY_IMAGE_ENV}`
- Image environment variable can have default value if empty using syntax: `${MY_IMAGE_ENV|quay.io/microcks/microcks-uber:1.15.0}`

### Manual Container

When you need to **manually configure container** with specific options, you can provide such container as instance that will be used by `@TestcontainersMicrocks`,
this can be done using `@ContainerMicrocks` annotation for container.

Example:
```java
@TestcontainersMicrocks(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @ContainerMicrocks
    private static final MicrocksContainer container = new MicrocksContainer("quay.io/microcks/microcks-uber:1.15.0")
            .withNetworkAliases("mymicrocks");
    
    @Test
    void test(@ConnectionMicrocks MicrocksConnection connection) {
        assertEquals("mymicrocks", connection.paramsInNetwork().get().host());
    }
}
```

### Network

In case you want to enable [Network.SHARED](https://java.testcontainers.org/features/networking/) for containers you can do this using `network` & `shared` parameter in annotation:
```java
@TestcontainersMicrocks(network = @Network(shared = true))
class ExampleTests {

    @Test
    void test() {
        // test
    }
}
```

`Default alias` will be created by default, even if nothing was specified (depends on implementation).

You can provide also custom alias for container.
Alias can be extracted from environment variable also or default value can be provided if environment is missing.

In case specified environment variable is missing `default alias` will be created:
```java
@TestcontainersMicrocks(network = @Network(alias = "${MY_ALIAS_ENV|my_default_alias}"))
class ExampleTests {

    @Test
    void test() {
        // test
    }
}
```

Image syntax:

- Image can have static value: `my-alias`
- Image can be provided via environment variable using syntax: `${MY_ALIAS_ENV}`
- Image environment variable can have default value if empty using syntax: `${MY_ALIAS_ENV|my-alias-default}`

### Annotation Connection

`MicrocksConnection` - can be injected to field or method parameter and used to communicate with running container via `@ConnectionMicrocks` annotation.
`MicrocksConnection` provides connection parameters, contract import and mock endpoint resolution for easier testing.

Example:
```java
@TestcontainersMicrocks(mode = ContainerMode.PER_CLASS, image = "quay.io/microcks/microcks-uber:1.15.0")
class ExampleTests {

    @ConnectionMicrocks
    private MicrocksConnection connection;

    @Test
    void test() {
        connection.importAsMainArtifact(new File("src/test/resources/hello-openapi.yaml"));
        assertFalse(connection.getServices().isEmpty());
    }
}
```

### External Connection

In case you want to use some external Microcks instance that is running in CI or other place for tests (due to docker limitations or other), 
you can use special *environment variables* and extension will use them to propagate connection and no Microcks containers will be running in such case.

Special environment variables:
- `EXTERNAL_TEST_MICROCKS_HOST` - Microcks instance host.
- `EXTERNAL_TEST_MICROCKS_PORT` - Microcks instance HTTP port.
- `EXTERNAL_TEST_MICROCKS_GRPC_PORT` - Microcks instance gRPC port (optional, defaults to `9090`).

## License

This project licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.
