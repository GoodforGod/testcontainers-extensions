# Testcontainers Extensions WireMock

[![Minimum required Java version](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Maven Central](https://maven-badges.herokuRpp.com/maven-central/io.goodforgod/testcontainers-extensions-wiremock.svg)](https://central.sonatype.com/artifact/io.goodforgod/testcontainers-extensions-wiremock)
[![GitHub Action](https://github.com/goodforgod/testcontainers-extensions/workflows/Release/badge.svg)](https://github.com/GoodforGod/testcontainers-extensions/actions?query=workflow%3A"CI+Master"++)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=coverage)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=ncloc)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)

Testcontainers [WireMock](https://wiremock.org/) Extension with advanced testing capabilities.

WireMock is an HTTP API mocking tool for stubbing responses, verifying requests, fault injection and proxying.

Features:
- Container easy run *per method*, *per class*, *per execution*.
- Container easy connection injection with asserts.

## Dependency :rocket:

**Gradle**
```groovy
testImplementation "io.goodforgod:testcontainers-extensions-wiremock:0.16.0"
```

**Maven**
```xml
<dependency>
    <groupId>io.goodforgod</groupId>
    <artifactId>testcontainers-extensions-wiremock</artifactId>
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

Test with container start in `PER_RUN` mode will look like:

```java
@TestcontainersWireMock(mode = ContainerMode.PER_RUN)
class ExampleTests {

  @ConnectionWireMock
  private WireMockConnection connection;

  @Test
  void test() {
    connection.client().register(get(urlEqualTo("/get"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("OK")));
  }
}
```

The `client()` is a `com.github.tomakehurst.wiremock.client.WireMock` admin client bound to the running
container. Use the static imports from `com.github.tomakehurst.wiremock.client.WireMock` (`get`, `post`,
`urlEqualTo`, `aResponse`, ...) to build stubs and verifications.

## Connection

`WireMockConnection` is an abstraction to manipulate container connection settings and drive stubbing.
You can inject connection via `@ConnectionWireMock` as field or method argument or manually create it from container or manual settings.

```java
class ExampleTests {

  @ConnectionWireMock 
  private WireMockConnection connection;
  
  @Test
  void test() {
    connection.client().register(get(urlEqualTo("/get"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("OK")));

    // base URL of the running mock, useful for the code under test
    URI baseUrl = connection.params().uri();
  }
}
```

## Annotation

`@TestcontainersWireMock` - allow **automatically start container** with specified image in different modes without the need to configure it.

Available containers modes:

- `PER_RUN` - start container one time per *test execution*. (Containers must have same instance, e.g. compare by `==`)
- `PER_CLASS` - start new container each *test class*.
- `PER_METHOD` - start new container each *test method*.

Simple example on how to start container per class, **no need to configure** container:
```java
@TestcontainersWireMock(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @Test
    void test(@ConnectionWireMock WireMockConnection connection) {
        assertNotNull(connection);
    }
}
```

**That's all** you need.

It is possible to customize image with annotation `image` parameter.

Image also can be provided from environment variable:
```java
@TestcontainersWireMock(image = "${MY_IMAGE_ENV|wiremock/wiremock:3.13.2}")
class ExampleTests {

    @Test
    void test() {
        // test
    }
}
```

Image syntax:

- Image can have static value: `wiremock/wiremock:3.13.2`
- Image can be provided via environment variable using syntax: `${MY_IMAGE_ENV}`
- Image environment variable can have default value if empty using syntax: `${MY_IMAGE_ENV|wiremock/wiremock:3.13.2}`

### Manual Container

When you need to **manually configure container** with specific options, you can provide such container as instance that will be used by `@TestcontainersWireMock`,
this can be done using `@ContainerWireMock` annotation for container.

Example:
```java
@TestcontainersWireMock(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @ContainerWireMock
    private static final WireMockContainer container = new WireMockContainer("wiremock/wiremock:3.13.2")
            .withNetworkAliases("mywiremock");
    
    @Test
    void test(@ConnectionWireMock WireMockConnection connection) {
        assertEquals("mywiremock", connection.paramsInNetwork().get().host());
    }
}
```

### Network

In case you want to enable [Network.SHARED](https://java.testcontainers.org/features/networking/) for containers you can do this using `network` & `shared` parameter in annotation:
```java
@TestcontainersWireMock(network = @Network(shared = true))
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
@TestcontainersWireMock(network = @Network(alias = "${MY_ALIAS_ENV|my_default_alias}"))
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

`WireMockConnection` - can be injected to field or method parameter and used to communicate with running container via `@ConnectionWireMock` annotation.
`WireMockConnection` provides connection parameters and the WireMock admin client for easier testing.

Example:
```java
@TestcontainersWireMock(mode = ContainerMode.PER_CLASS, image = "wiremock/wiremock:3.13.2")
class ExampleTests {

    @ConnectionWireMock
    private WireMockConnection connection;

    @Test
    void test() {
        connection.client().register(get(urlEqualTo("/get"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("OK")));
    }
}
```

### External Connection

In case you want to use some external WireMock instance that is running in CI or other place for tests (due to docker limitations or other), 
you can use special *environment variables* and extension will use them to propagate connection and no WireMock containers will be running in such case.

Special environment variables:
- `EXTERNAL_TEST_WIREMOCK_HOST` - WireMock instance host.
- `EXTERNAL_TEST_WIREMOCK_PORT` - WireMock instance port.

## License

This project licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.
