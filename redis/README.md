# Testcontainers Extensions Redis

[![Minimum required Java version](https://img.shields.io/badge/Java-11%2B-blue?logo=openjdk)](https://openjdk.org/projects/jdk/11/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.goodforgod/testcontainers-extensions-redis/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.goodforgod/testcontainers-extensions-redis)
[![GitHub Action](https://github.com/goodforgod/testcontainers-extensions/workflows/Java%20CI/badge.svg)](https://github.com/GoodforGod/testcontainers-extensions/actions?query=workflow%3A%22Java+CI%22)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=coverage)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=ncloc)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)

Testcontainers Redis Extension with advanced testing capabilities.

Features:
- Container easy run *per method*, *per class*, *per execution*.
- Container easy migration with scripts.
- Container easy connection injection with asserts.

## Dependency :rocket:

**Gradle**
```groovy
testImplementation "io.goodforgod:testcontainers-extensions-redis:0.4.0"
```

**Maven**
```xml
<dependency>
    <groupId>io.goodforgod</groupId>
    <artifactId>testcontainers-extensions-redis</artifactId>
    <version>0.4.0</version>
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
- [Container](#container)
  - [Preconfigured container](#preconfigured-container)
- [Connection](#connection)
  - [External Connection](#external-connection)
- [Migration](#migration)

## Container

`@TestcontainersRedis` - provides container start in different modes per test class.

Available containers modes:
- `PER_RUN` - start container one time per *test execution*. (Containers should have same image to be reused between test classes)
- `PER_CLASS` - start new container each *test class*.
- `PER_METHOD` - start new container each *test method*.

Simple example on how to start container per class:
```java
@TestcontainersRedis(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @Test
    void test(@ContainerRedisConnection RedisConnection connection) {
        assertNotNull(connection);
    }
}
```

It is possible to customize image with annotation `image` parameter.

### Preconfigured container

Container instance can be used by extensions via `@ContainerRedis` annotation.

Example:
```java
@TestcontainersRedis(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @ContainerRedis
    private static final RedisContainer container = new RedisContainer()
            .withNetworkAliases("myredis")
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(RedisContainer.class)))
            .withNetwork(Network.SHARED);
    
    @Test
    void test(@ContainerRedisConnection RedisConnection connection) {
        assertEquals("myredis", connection.paramsInNetwork().get().host());
    }
}
```

## Connection

`RedisConnection` - can be injected to field or method parameter and used to communicate with running container via `@ContainerRedisConnection` annotation.
`RedisConnection` provides connection parameters, useful asserts, checks, etc. for easier testing.

Example:
```java
@TestcontainersRedis(mode = ContainerMode.PER_CLASS, image = "redis:7.0-alpine")
class ExampleTests {

    @ContainerRedisConnection
    private RedisConnection connectionInField;

    @Test
    void test(@ContainerRedisConnection RedisConnection connection) {
        connection.commands().sadd("11", "1");
        connection.commands().sadd("12", "2");
        assertEquals(2, connection.countPrefix("1"));
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