# Testcontainers Extensions Redis

[![Minimum required Java version](https://img.shields.io/badge/Java-11%2B-blue?logo=openjdk)](https://openjdk.org/projects/jdk/11/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.goodforgod/testcontainers-extensions-redis/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.goodforgod/testcontainers-extensions-redis)
[![GitHub Action](https://github.com/goodforgod/testcontainers-extensions/workflows/Release/badge.svg)](https://github.com/GoodforGod/testcontainers-extensions/actions?query=workflow%3A"CI+Master"++)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=coverage)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=ncloc)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)

Testcontainers Redis Extension with advanced testing capabilities.

Features:
- Container easy run *per method*, *per class*, *per execution*.
- Container easy connection injection with asserts.

## Dependency :rocket:

**Gradle**
```groovy
testImplementation "io.goodforgod:testcontainers-extensions-redis:0.11.0"
```

**Maven**
```xml
<dependency>
    <groupId>io.goodforgod</groupId>
    <artifactId>testcontainers-extensions-redis</artifactId>
    <version>0.11.0</version>
    <scope>test</scope>
</dependency>
```

### Redis Driver
[Redis Jedis Client](https://mvnrepository.com/artifact/redis.clients/jedis) must be on classpath, if it is somehow not on your classpath already,
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
@TestcontainersRedis(mode = ContainerMode.PER_RUN)
class ExampleTests {

  @ConnectionRedis 
  private RedisConnection connection;
  
  @Test
  void test() {
    connection.commands().set("11", "1");
    connection.commands().set("12", "2");
    assertEquals(2, connection.countPrefix(RedisKey.of("1")));
  }
}
```

## Connection

`RedisConnection` is an abstraction with asserting data in database container and easily manipulate container connection settings.
You can inject connection via `@ConnectionRedis` as field or method argument or manually create it from container or manual settings.

```java
class ExampleTests {

  private static final RedisContainer container = new RedisContainer();

  @Test
  void test() {
    container.start();
    RedisConnection connection = RedisConnection.forContainer(container);
    connection.commands().set("11", "1");
    connection.commands().set("12", "2");
    assertEquals(2, connection.countPrefix(RedisKey.of("1")));
  }
}
```

## Annotation

`@TestcontainersRedis` - allow **automatically start container** with specified image in different modes without the need to configure it.

Available containers modes:

- `PER_RUN` - start container one time per *test execution*. (Containers must have same instance, e.g. compare by `==`)
- `PER_CLASS` - start new container each *test class*.
- `PER_METHOD` - start new container each *test method*.

Simple example on how to start container per class, **no need to configure** container:
```java
@TestcontainersRedis(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @Test
    void test(@ConnectionRedis RedisConnection connection) {
        assertNotNull(connection);
    }
}
```

**That's all** you need.

It is possible to customize image with annotation `image` parameter.

Image also can be provided from environment variable:
```java
@TestcontainersRedis(image = "${MY_IMAGE_ENV|redis:7.2-alpine}")
class ExampleTests {

    @Test
    void test() {
        // test
    }
}
```

Image syntax:

- Image can have static value: `redis:7.2-alpine`
- Image can be provided via environment variable using syntax: `${MY_IMAGE_ENV}`
- Image environment variable can have default value if empty using syntax: `${MY_IMAGE_ENV|redis:7.2-alpine}`

### Manual Container

When you need to **manually configure container** with specific options, you can provide such container as instance that will be used by `@TestcontainersRedis`,
this can be done using `@ContainerRedis` annotation for container.

Example:
```java
@TestcontainersRedis(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @ContainerRedis
    private static final RedisContainer container = new RedisContainer().withNetworkAliases("myredis");
    
    @Test
    void test(@ConnectionRedis RedisConnection connection) {
        assertEquals("myredis", connection.paramsInNetwork().get().host());
    }
}
```

### Network

In case you want to enable [Network.SHARED](https://java.testcontainers.org/features/networking/) for containers you can do this using `network` & `shared` parameter in annotation:
```java
@TestcontainersRedis(network = @Network(shared = true))
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
@TestcontainersRedis(network = @Network(alias = "${MY_ALIAS_ENV|my_default_alias}"))
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

`RedisConnection` - can be injected to field or method parameter and used to communicate with running container via `@ConnectionRedis` annotation.
`RedisConnection` provides connection parameters, useful asserts, checks, etc. for easier testing.

Example:
```java
@TestcontainersRedis(mode = ContainerMode.PER_CLASS, image = "redis:7.2-alpine")
class ExampleTests {

    @ConnectionRedis
    private RedisConnection connection;

    @Test
    void test() {
        connection.commands().set("11", "1");
        connection.commands().set("12", "2");
        assertEquals(2, connection.countPrefix(RedisKey.of("1")));
    }
}
```

### External Connection

In case you want to use some external Redis instance that is running in CI or other place for tests (due to docker limitations or other), 
you can use special *environment variables* and extension will use them to propagate connection and no Redis containers will be running in such case.

Special environment variables:
- `EXTERNAL_TEST_REDIS_USERNAME` - Redis instance username (optional).
- `EXTERNAL_TEST_REDIS_PASSWORD` - Redis instance password (optional).
- `EXTERNAL_TEST_REDIS_HOST` - Redis instance host.
- `EXTERNAL_TEST_REDIS_PORT` - Redis instance port.
- `EXTERNAL_TEST_REDIS_DATABASE` - Redis instance database (`0` by default).

## License

This project licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.
