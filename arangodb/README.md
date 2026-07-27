# Testcontainers Extensions ArangoDB

[![Minimum required Java version](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.goodforgod/testcontainers-extensions-arangodb.svg)](https://central.sonatype.com/artifact/io.goodforgod/testcontainers-extensions-arangodb)
[![GitHub Action](https://github.com/goodforgod/testcontainers-extensions/workflows/Release/badge.svg)](https://github.com/GoodforGod/testcontainers-extensions/actions?query=workflow%3A"CI+Master"++)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=coverage)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=ncloc)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)

Testcontainers ArangoDB Extension with advanced testing capabilities.

Features:
- Container easy run *per method*, *per class*, *per execution*.
- Container easy connection injection.
- ArangoDB container implementation with auth configuration.

## Dependency :rocket:

**Gradle**
```groovy
testImplementation "io.goodforgod:testcontainers-extensions-arangodb:0.15.0"
```

**Maven**
```xml
<dependency>
    <groupId>io.goodforgod</groupId>
    <artifactId>testcontainers-extensions-arangodb</artifactId>
    <version>0.15.0</version>
    <scope>test</scope>
</dependency>
```

## Usage

Test with container start in `PER_RUN` mode will look like:

```java
@TestcontainersArango(mode = ContainerMode.PER_RUN)
class ExampleTests {

    @ConnectionArango
    private ArangoConnection connection;

    @Test
    void test() {
        assertNotNull(connection.client().getVersion());
    }
}
```

## Connection

`ArangoConnection` can be injected via `@ConnectionArango` as field or method argument or manually created from container or manual settings.
It provides connection parameters for host access, optional Docker network parameters, and `com.arangodb.ArangoDB` client.

```java
@TestcontainersArango(mode = ContainerMode.PER_CLASS, password = "test")
class ExampleTests {

    @Test
    void test(@ConnectionArango ArangoConnection connection) {
        assertEquals("root", connection.params().username());
    }
}
```

## Annotation

`@TestcontainersArango` starts `ArangoContainer` with specified image in different modes.

Available containers modes:

- `PER_RUN` - start container one time per *test execution*.
- `PER_CLASS` - start new container each *test class*.
- `PER_METHOD` - start new container each *test method*.

Image can be customized:

```java
@TestcontainersArango(image = "${MY_IMAGE_ENV|arangodb:3.12.4}")
class ExampleTests {

    @Test
    void test(@ConnectionArango ArangoConnection connection) {
        assertNotNull(connection);
    }
}
```

Image syntax:

- Image can have static value: `arangodb:3.12.4`
- Image can be provided via environment variable using syntax: `${MY_IMAGE_ENV}`
- Image environment variable can have default value if empty using syntax: `${MY_IMAGE_ENV|arangodb:3.12.4}`

### Manual Container

When you need to manually configure container with specific options, provide container instance using `@ContainerArango`.

```java
import io.testcontainers.arangodb.containers.ArangoContainer;

@TestcontainersArango(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @ContainerArango
    private static final ArangoContainer container = new ArangoContainer("arangodb:3.12.4")
            .withPassword("test")
            .withNetworkAliases("my-arango");

    @Test
    void test(@ConnectionArango ArangoConnection connection) {
        assertEquals("my-arango", connection.paramsInNetwork().get().host());
    }
}
```

### Network

In case you want to enable [Network.SHARED](https://java.testcontainers.org/features/networking/) for containers you can do this using `network` & `shared` parameter in annotation:

```java
@TestcontainersArango(network = @Network(shared = true))
class ExampleTests {

    @Test
    void test() {
        // test
    }
}
```

### External Connection

In case you want to use some external ArangoDB instance that is running in CI or other place for tests,
you can use special *environment variables* and extension will use them to propagate connection and no ArangoDB containers will be running in such case.

Special environment variables:
- `EXTERNAL_TEST_ARANGODB_HOST` - ArangoDB instance host.
- `EXTERNAL_TEST_ARANGODB_PORT` - ArangoDB instance port.
- `EXTERNAL_TEST_ARANGODB_USERNAME` - ArangoDB username, `root` by default.
- `EXTERNAL_TEST_ARANGODB_PASSWORD` - ArangoDB password.

## License

This project licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.
