# Testcontainers Extensions Valkey

[![Minimum required Java version](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Maven Central](https://img.shields.io/maven-central/v/io.goodforgod/testcontainers-extensions-valkey.svg)](https://central.sonatype.com/artifact/io.goodforgod/testcontainers-extensions-valkey)
[![GitHub Action](https://github.com/goodforgod/testcontainers-extensions/workflows/Release/badge.svg)](https://github.com/GoodforGod/testcontainers-extensions/actions?query=workflow%3A"CI+Master"++)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=coverage)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=ncloc)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)

Testcontainers Valkey Extension with advanced testing capabilities.

Features:
- Container easy run *per method*, *per class*, *per execution*.
- Container easy connection injection with asserts.

## Dependency :rocket:

**Gradle**
```groovy
testImplementation "io.goodforgod:testcontainers-extensions-valkey:0.14.0"
```

**Maven**
```xml
<dependency>
    <groupId>io.goodforgod</groupId>
    <artifactId>testcontainers-extensions-valkey</artifactId>
    <version>0.14.0</version>
    <scope>test</scope>
</dependency>
```

### Valkey Client
[Valkey-compatible Jedis Client](https://mvnrepository.com/artifact/redis.clients/jedis) must be on classpath, if it is somehow not on your classpath already,
don't forget to add:

**Gradle**
```groovy
testImplementation "redis.clients:jedis:4.4.3"
```

**Maven**
```xml
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>4.4.3</version>
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
@TestcontainersValkey(mode = ContainerMode.PER_RUN)
class ExampleTests {

  @ConnectionValkey 
  private ValkeyConnection connection;
  
  @Test
  void test() {
    connection.commands().set("11", "1");
    connection.commands().set("12", "2");
    assertEquals(2, connection.countPrefix(ValkeyKey.of("1")));
  }
}
```

## Connection

`ValkeyConnection` is an abstraction with asserting data in database container and easily manipulate container connection settings.
You can inject connection via `@ConnectionValkey` as field or method argument or manually create it from container or manual settings.

```java
class ExampleTests {

  private static final ValkeyContainer container = new ValkeyContainer();

  @Test
  void test() {
    container.start();
    ValkeyConnection connection = ValkeyConnection.forContainer(container);
    connection.commands().set("11", "1");
    connection.commands().set("12", "2");
    assertEquals(2, connection.countPrefix(ValkeyKey.of("1")));
  }
}
```

## Annotation

`@TestcontainersValkey` - allow **automatically start container** with specified image in different modes without the need to configure it.

Available containers modes:

- `PER_RUN` - start container one time per *test execution*. (Containers must have same instance, e.g. compare by `==`)
- `PER_CLASS` - start new container each *test class*.
- `PER_METHOD` - start new container each *test method*.

Simple example on how to start container per class, **no need to configure** container:
```java
@TestcontainersValkey(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @Test
    void test(@ConnectionValkey ValkeyConnection connection) {
        assertNotNull(connection);
    }
}
```

**That's all** you need.

It is possible to customize image with annotation `image` parameter.

Image also can be provided from environment variable:
```java
@TestcontainersValkey(image = "${MY_IMAGE_ENV|valkey/valkey:8.1-alpine}")
class ExampleTests {

    @Test
    void test() {
        // test
    }
}
```

Image syntax:

- Image can have static value: `valkey/valkey:8.1-alpine`
- Image can be provided via environment variable using syntax: `${MY_IMAGE_ENV}`
- Image environment variable can have default value if empty using syntax: `${MY_IMAGE_ENV|valkey/valkey:8.1-alpine}`

### Manual Container

When you need to **manually configure container** with specific options, you can provide such container as instance that will be used by `@TestcontainersValkey`,
this can be done using `@ContainerValkey` annotation for container.

Example:
```java
@TestcontainersValkey(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @ContainerValkey
    private static final ValkeyContainer container = new ValkeyContainer().withNetworkAliases("myredis");
    
    @Test
    void test(@ConnectionValkey ValkeyConnection connection) {
        assertEquals("myredis", connection.paramsInNetwork().get().host());
    }
}
```

### Network

In case you want to enable [Network.SHARED](https://java.testcontainers.org/features/networking/) for containers you can do this using `network` & `shared` parameter in annotation:
```java
@TestcontainersValkey(network = @Network(shared = true))
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
@TestcontainersValkey(network = @Network(alias = "${MY_ALIAS_ENV|my_default_alias}"))
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

`ValkeyConnection` - can be injected to field or method parameter and used to communicate with running container via `@ConnectionValkey` annotation.
`ValkeyConnection` provides connection parameters, useful asserts, checks, etc. for easier testing.

Example:
```java
@TestcontainersValkey(mode = ContainerMode.PER_CLASS, image = "valkey/valkey:8.1-alpine")
class ExampleTests {

    @ConnectionValkey
    private ValkeyConnection connection;

    @Test
    void test() {
        connection.commands().set("11", "1");
        connection.commands().set("12", "2");
        assertEquals(2, connection.countPrefix(ValkeyKey.of("1")));
    }
}
```

### External Connection

In case you want to use some external Valkey instance that is running in CI or other place for tests (due to docker limitations or other), 
you can use special *environment variables* and extension will use them to propagate connection and no Valkey containers will be running in such case.

Special environment variables:
- `EXTERNAL_TEST_VALKEY_USERNAME` - Valkey instance username (optional).
- `EXTERNAL_TEST_VALKEY_PASSWORD` - Valkey instance password (optional).
- `EXTERNAL_TEST_VALKEY_HOST` - Valkey instance host.
- `EXTERNAL_TEST_VALKEY_PORT` - Valkey instance port.
- `EXTERNAL_TEST_VALKEY_DATABASE` - Valkey instance database (`0` by default).

## License

This project licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.


