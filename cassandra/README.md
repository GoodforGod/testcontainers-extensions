# Testcontainers Extensions Cassandra

[![Minimum required Java version](https://img.shields.io/badge/Java-11%2B-blue?logo=openjdk)](https://openjdk.org/projects/jdk/11/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.goodforgod/testcontainers-extensions-cassandra/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.goodforgod/testcontainers-extensions-cassandra)
[![GitHub Action](https://github.com/goodforgod/testcontainers-extensions/workflows/Release/badge.svg)](https://github.com/GoodforGod/testcontainers-extensions/actions?query=workflow%3A%22Java+CI%22)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=coverage)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=ncloc)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)

Testcontainers Cassandra Extension with advanced testing capabilities.

Features:
- Container easy run *per method*, *per class*, *per execution*.
- Container easy migration with scripts.
- Container easy connection injection with asserts.

## Dependency :rocket:

**Gradle**
```groovy
testImplementation "io.goodforgod:testcontainers-extensions-cassandra:0.4.1"
```

**Maven**
```xml
<dependency>
    <groupId>io.goodforgod</groupId>
    <artifactId>testcontainers-extensions-cassandra</artifactId>
    <version>0.4.1</version>
    <scope>test</scope>
</dependency>
```

### Cassandra Driver
[Cassandra DataStax Driver](https://mvnrepository.com/artifact/com.datastax.oss/java-driver-core) must be on classpath, if it is somehow not on your classpath already,
don't forget to add:

**Gradle**
```groovy
testImplementation "com.datastax.oss:java-driver-core:4.17.0"
```

**Maven**
```xml
<dependency>
    <groupId>com.datastax.oss</groupId>
    <artifactId>java-driver-core</artifactId>
    <version>4.17.0</version>
    <scope>test</scope>
</dependency>
```

## Content
- [Container](#container)
  - [Preconfigured container](#preconfigured-container)
  - [Container Old Driver](#container-old-driver)
- [Connection](#connection)
  - [External Connection](#external-connection)
- [Migration](#migration)

## Container

`@TestcontainersCassandra` - provides container start in different modes per test class.

Available containers modes:
- `PER_RUN` - start container one time per *test execution*. (Containers should have same image to be reused between test classes)
- `PER_CLASS` - start new container each *test class*.
- `PER_METHOD` - start new container each *test method*.

Simple example on how to start container per class:
```java
@TestcontainersCassandra(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @Test
    void test(@ContainerCassandraConnection CassandraConnection connection) {
        assertNotNull(connection);
    }
}
```

It is possible to customize image with annotation `image` parameter.

### Preconfigured container

Container instance can be used by extensions via `@ContainerCassandra` annotation.

Example:
```java
@TestcontainersCassandra(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @ContainerCassandra
    private static final CassandraContainer<?> container = new CassandraContainer<>()
            .withEnv("CASSANDRA_DC", "mydc")
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(CassandraContainer.class)))
            .withNetwork(Network.SHARED);
    
    @Test
    void test(@ContainerCassandraConnection CassandraConnection connection) {
      assertEquals("mydc", connection.params().datacenter());
    }
}
```

## Container Old Driver

[Testcontainers Cassandra module](https://java.testcontainers.org/modules/databases/cassandra/) leaks old driver as transitive dependency and uses it in its deprecated APIs.

Library excludes [com.datastax.cassandra:cassandra-driver-core](https://mvnrepository.com/artifact/com.datastax.cassandra/cassandra-driver-core/3.10.0)
old driver from dependency leaking due to lots of vulnerabilities, if you require it add such dependency manually yourself.

## Connection

`CassandraConnection` - can be injected to field or method parameter and used to communicate with running container via `@ContainerCassandraConnection` annotation.
`CassandraConnection` provides connection parameters, useful asserts, checks, etc. for easier testing.

Example:
```java
@TestcontainersCassandra(mode = ContainerMode.PER_CLASS, image = "cassandra:4.1")
class ExampleTests {

    @ContainerCassandraConnection
    private CassandraConnection connectionInField;

    @Test
    void test(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO cassandra.users(id) VALUES(2);");
        var usersFound = connection.queryMany("SELECT * FROM cassandra.users;", r -> r.getInt(0));
        assertEquals(2, usersFound.size());
    }
}
```

### External Connection

In case you want to use some external Cassandra instance that is running in CI or other place for tests (due to docker limitations or other), 
you can use special *environment variables* and extension will use them to propagate connection and no Cassandra containers will be running in such case.

Special environment variables:
- `EXTERNAL_TEST_CASSANDRA_USERNAME` - Cassandra instance username (optional).
- `EXTERNAL_TEST_CASSANDRA_PASSWORD` - Cassandra instance password (optional).
- `EXTERNAL_TEST_CASSANDRA_HOST` - Cassandra instance host.
- `EXTERNAL_TEST_CASSANDRA_PORT` - Cassandra instance port.
- `EXTERNAL_TEST_CASSANDRA_DATACENTER` - Cassandra instance database (`datacenter1` by default).

## Migration

`@Migrations` allow easily migrate database between test executions and drop after tests.

Annotation parameters:
- `engine` - to use for migration.
- `apply` - parameter configures migration mode.
- `drop` - configures when to reset/drop/clear database.

Available migration engines:
- Scripts - For `apply` load scripts from specified paths or directories and execute in ASC order, for `drop` clean all Non System tables in all cassandra


Given engine is Scripts and migration file named `setup.sql` is in resource directory `migration`:
```sql
CREATE KEYSPACE IF NOT EXISTS cassandra WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE IF NOT EXISTS cassandra.users
(
  id INT,
  PRIMARY KEY (id)
);
```

Test with container and migration per method will look like:
```java
@TestcontainersCassandra(mode = ContainerMode.PER_CLASS,
        migration = @Migration(
                engine = Migration.Engines.SCRIPTS,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD,
                migrations = { "migration/setup.cql" }
        ))
class ExampleTests {

    @Test
    void test(@ContainerCassandraConnection CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO cassandra.users(id) VALUES(2);");
        var usersFound = connection.queryMany("SELECT * FROM cassandra.users;", r -> r.getInt(0));
        assertEquals(2, usersFound.size());
    }
}
```

## License

This project licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.
