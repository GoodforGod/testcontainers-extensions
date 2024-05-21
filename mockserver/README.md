# Testcontainers Extensions MockServer

[![Minimum required Java version](https://img.shields.io/badge/Java-11%2B-blue?logo=openjdk)](https://openjdk.org/projects/jdk/11/)
[![Maven Central](https://maven-badges.herokuRpp.com/maven-central/io.goodforgod/testcontainers-extensions-mockserver/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.goodforgod/testcontainers-extensions-mockserver)
[![GitHub Action](https://github.com/goodforgod/testcontainers-extensions/workflows/Release/badge.svg)](https://github.com/GoodforGod/testcontainers-extensions/actions?query=workflow%3A"CI+Master"++)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=coverage)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=ncloc)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)

Testcontainers MockServer Extension with advanced testing capabilities.

Features:
- Container easy run *per method*, *per class*, *per execution*.
- Container easy connection injection with asserts.

## Dependency :rocket:

**Gradle**
```groovy
testImplementation "io.goodforgod:testcontainers-extensions-mockserver:0.12.0"
```

**Maven**
```xml
<dependency>
    <groupId>io.goodforgod</groupId>
    <artifactId>testcontainers-extensions-mockserver</artifactId>
    <version>0.12.0</version>
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
@TestcontainersMockServer(mode = ContainerMode.PER_RUN)
class ExampleTests {

  @ConnectionMockServer
  private MockServerConnection connection;

  @Test
  void test() {
    connection.client().when(HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/get"))
            .respond(HttpResponse.response()
                    .withStatusCode(200)
                    .withBody("OK"));
  }
}
```

## Connection

`MockServerConnection` is an abstraction with asserting data in database container and easily manipulate container connection settings.
You can inject connection via `@ConnectionMockServer` as field or method argument or manually create it from container or manual settings.

```java
class ExampleTests {

  @ConnectionMockServer 
  private MockServerConnection connection;
  
  @Test
  void test() {
    connection().client().when(HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/get"))
            .respond(HttpResponse.response()
                    .withStatusCode(200)
                    .withBody("OK"));
  }
}
```

## Annotation

`@TestcontainersMockServer` - allow **automatically start container** with specified image in different modes without the need to configure it.

Available containers modes:

- `PER_RUN` - start container one time per *test execution*. (Containers must have same instance, e.g. compare by `==`)
- `PER_CLASS` - start new container each *test class*.
- `PER_METHOD` - start new container each *test method*.

Simple example on how to start container per class, **no need to configure** container:
```java
@TestcontainersMockServer(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @Test
    void test(@ConnectionMockServer MockServerConnection connection) {
        assertNotNull(connection);
    }
}
```

**That's all** you need.

It is possible to customize image with annotation `image` parameter.

Image also can be provided from environment variable:
```java
@TestcontainersMockServer(image = "${MY_IMAGE_ENV|mockserver/mockserver:5.15.0}")
class ExampleTests {

    @Test
    void test() {
        // test
    }
}
```

Image syntax:

- Image can have static value: `mockserver/mockserver:5.15.0`
- Image can be provided via environment variable using syntax: `${MY_IMAGE_ENV}`
- Image environment variable can have default value if empty using syntax: `${MY_IMAGE_ENV|mockserver/mockserver:5.15.0}`

### Manual Container

When you need to **manually configure container** with specific options, you can provide such container as instance that will be used by `@TestcontainersMockServer`,
this can be done using `@ContainerMockServer` annotation for container.

Example:
```java
@TestcontainersMockServer(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @ContainerMockServer
    private static final MockServerContainer container = new MockServerContainer().withNetworkAliases("mymockserver");
    
    @Test
    void test(@ConnectionMockServer MockServerConnection connection) {
        assertEquals("mymockserver", connection.paramsInNetwork().get().host());
    }
}
```

### Network

In case you want to enable [Network.SHARED](https://java.testcontainers.org/features/networking/) for containers you can do this using `network` & `shared` parameter in annotation:
```java
@TestcontainersMockServer(network = @Network(shared = true))
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
@TestcontainersMockServer(network = @Network(alias = "${MY_ALIAS_ENV|my_default_alias}"))
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

`MockServerConnection` - can be injected to field or method parameter and used to communicate with running container via `@ConnectionMockServer` annotation.
`MockServerConnection` provides connection parameters, useful asserts, checks, etc. for easier testing.

Example:
```java
@TestcontainersMockServer(mode = ContainerMode.PER_CLASS, image = "mockserver/mockserver:5.15.0")
class ExampleTests {

    @ConnectionMockServer
    private MockServerConnection connection;

    @Test
    void test() {
        connection.client().when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/get"))
                .respond(HttpResponse.response()
                        .withStatusCode(200)
                        .withBody("OK"));
    }
}
```

### External Connection

In case you want to use some external MockServer instance that is running in CI or other place for tests (due to docker limitations or other), 
you can use special *environment variables* and extension will use them to propagate connection and no MockServer containers will be running in such case.

Special environment variables:
- `EXTERNAL_TEST_MOCKSERVER_HOST` - MockServer instance host.
- `EXTERNAL_TEST_MOCKSERVER_PORT` - MockServer instance port.

## License

This project licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.
