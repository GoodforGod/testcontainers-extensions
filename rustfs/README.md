# Testcontainers Extensions RustFS

[![Minimum required Java version](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Maven Central](https://maven-badges.herokuRpp.com/maven-central/io.goodforgod/testcontainers-extensions-rustfs.svg)](https://central.sonatype.com/artifact/io.goodforgod/testcontainers-extensions-rustfs)
[![GitHub Action](https://github.com/goodforgod/testcontainers-extensions/workflows/Release/badge.svg)](https://github.com/GoodforGod/testcontainers-extensions/actions?query=workflow%3A"CI+Master"++)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=coverage)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=ncloc)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)

Testcontainers [RustFS](https://hub.docker.com/r/rustfs/rustfs) Extension with advanced testing capabilities.

RustFS is a high-performance, S3 compatible object storage. As it is S3 compatible, this extension talks
to it using the MinIO Java client.

Features:
- Container easy run *per method*, *per class*, *per execution*.
- Container easy connection injection with asserts.

## Dependency :rocket:

**Gradle**
```groovy
testImplementation "io.goodforgod:testcontainers-extensions-rustfs:0.16.0"
```

**Maven**
```xml
<dependency>
    <groupId>io.goodforgod</groupId>
    <artifactId>testcontainers-extensions-rustfs</artifactId>
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
@TestcontainersRustFS(mode = ContainerMode.PER_RUN,
        bucket = @Bucket(
                create = Bucket.Mode.PER_METHOD,
                drop = Bucket.Mode.PER_METHOD,
                value = { "my-bucket" }))
class ExampleTests {

  @ConnectionRustFS
  private RustFSConnection connection;

  @Test
  void test() {
      byte[] bytes = "someValue".getBytes(StandardCharsets.UTF_8);
      connection.client().putObject(PutObjectArgs.builder()
                .bucket("my-bucket")
                .object("someKey")
                .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                .build());
    }
}
```

## Connection

`RustFSConnection` is an abstraction with asserting data in storage container and easily manipulate container connection settings.
You can inject connection via `@ConnectionRustFS` as field or method argument or manually create it from container or manual settings.

```java
class ExampleTests {

  @ConnectionRustFS 
  private RustFSConnection connection;
  
  @Test
  void test() {
    byte[] bytes = "someValue".getBytes(StandardCharsets.UTF_8);
    connection.client().putObject(PutObjectArgs.builder()
            .bucket("my-bucket")
            .object("someKey")
            .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
            .build());
  }
}
```

## Annotation

`@TestcontainersRustFS` - allow **automatically start container** with specified image in different modes without the need to configure it.

Available containers modes:

- `PER_RUN` - start container one time per *test execution*. (Containers must have same instance, e.g. compare by `==`)
- `PER_CLASS` - start new container each *test class*.
- `PER_METHOD` - start new container each *test method*.

Simple example on how to start container per class, **no need to configure** container:
```java
@TestcontainersRustFS(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @Test
    void test(@ConnectionRustFS RustFSConnection connection) {
        assertNotNull(connection);
    }
}
```

**That's all** you need.

It is possible to customize image with annotation `image` parameter.

Image also can be provided from environment variable:
```java
@TestcontainersRustFS(image = "${MY_IMAGE_ENV|rustfs/rustfs:1.0.0}")
class ExampleTests {

    @Test
    void test() {
        // test
    }
}
```

Image syntax:

- Image can have static value: `rustfs/rustfs:1.0.0`
- Image can be provided via environment variable using syntax: `${MY_IMAGE_ENV}`
- Image environment variable can have default value if empty using syntax: `${MY_IMAGE_ENV|rustfs/rustfs:1.0.0}`

### Manual Container

When you need to **manually configure container** with specific options, you can provide such container as instance that will be used by `@TestcontainersRustFS`,
this can be done using `@ContainerRustFS` annotation for container.

Example:
```java
@TestcontainersRustFS(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @ContainerRustFS
    private static final RustFSContainer container = new RustFSContainer("rustfs/rustfs:1.0.0").withNetworkAliases("myRustFS");
    
    @Test
    void test(@ConnectionRustFS RustFSConnection connection) {
        assertEquals("myRustFS", connection.paramsInNetwork().get().host());
    }
}
```

### Network

In case you want to enable [Network.SHARED](https://java.testcontainers.org/features/networking/) for containers you can do this using `network` & `shared` parameter in annotation:
```java
@TestcontainersRustFS(network = @Network(shared = true))
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
@TestcontainersRustFS(network = @Network(alias = "${MY_ALIAS_ENV|my_default_alias}"))
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

`RustFSConnection` - can be injected to field or method parameter and used to communicate with running container via `@ConnectionRustFS` annotation.
`RustFSConnection` provides connection parameters, useful asserts, checks, etc. for easier testing.

Example:
```java
@TestcontainersRustFS(mode = ContainerMode.PER_CLASS, image = "rustfs/rustfs:1.0.0")
class ExampleTests {

    @ConnectionRustFS
    private RustFSConnection connection;

    @Test
    void test() {
      connection.client().makeBucket(MakeBucketArgs.builder()
              .bucket("my--test-bucket")
              .build());
    }
}
```

### External Connection

In case you want to use some external RustFS instance that is running in CI or other place for tests (due to docker limitations or other), 
you can use special *environment variables* and extension will use them to propagate connection and no RustFS containers will be running in such case.

Special environment variables:
- `EXTERNAL_TEST_RUSTFS_HOST` - RustFS instance host.
- `EXTERNAL_TEST_RUSTFS_PORT` - RustFS instance port.
- `EXTERNAL_TEST_RUSTFS_ACCESS_KEY` - RustFS access key.
- `EXTERNAL_TEST_RUSTFS_SECRET_KEY` - RustFS secret key.

## License

This project licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.
