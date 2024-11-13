# Testcontainers Extensions Cassandra

[![Minimum required Java version](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.goodforgod/testcontainers-extensions-cassandra/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.goodforgod/testcontainers-extensions-cassandra)
[![GitHub Action](https://github.com/goodforgod/testcontainers-extensions/workflows/Release/badge.svg)](https://github.com/GoodforGod/testcontainers-extensions/actions?query=workflow%3A"CI+Master"++)
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
testImplementation "io.goodforgod:testcontainers-extensions-cassandra:0.12.1"
```

**Maven**
```xml
<dependency>
    <groupId>io.goodforgod</groupId>
    <artifactId>testcontainers-extensions-cassandra</artifactId>
    <version>0.12.1</version>
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
- [Usage](#usage)
- [Old Driver](#container-old-driver)
- [Connection](#connection)
  - [Migration](#connection-migration)
- [Annotation](#annotation)
  - [Manual](#manual-container)
  - [Network](#network)
  - [Connection](#annotation-connection)
  - [Migration](#annotation-migration)
- [External Connection](#external-connection)

## Usage

Test with container start in `PER_RUN` mode and migration per method will look like:

```java
@TestcontainersCassandra(mode = ContainerMode.PER_RUN,
        migration = @Migration(
                engine = Migration.Engines.SCRIPTS,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD,
                migrations = { "migration/setup.cql" }
        ))
class ExampleTests {

    @Test
    void test(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO users(id) VALUES(1);");
        var usersFound = connection.queryMany("SELECT * FROM users;", r -> r.getInt(0));
        assertEquals(1, usersFound.size());
    }
}
```

## Container Old Driver

[Testcontainers Cassandra module](https://java.testcontainers.org/modules/databases/cassandra/) leaks old driver as transitive dependency and uses it in its deprecated APIs.

Library excludes [com.datastax.cassandra:cassandra-driver-core](https://mvnrepository.com/artifact/com.datastax.cassandra/cassandra-driver-core/3.10.0)
old driver from dependency leaking due to lots of vulnerabilities, if you require it add such dependency manually yourself.

## Connection

`CassandraConnection` is an abstraction with asserting data in database container and easily manipulate container connection settings.
You can inject connection via `@ConnectionCassandra` as field or method argument or manually create it from container or manual settings.

```java
class ExampleTests {

    private static final CassandraContainer<?> container = new CassandraContainer<>();
    
    @Test
    void test() {
      container.start();
      CassandraConnection connection = CassandraConnection.forContainer(container);
      connection.execute("INSERT INTO users VALUES(1);");
    }
}
```

Keyspace is created automatically.

### Connection Migration

`Migrations` allow easily migrate database between test executions and drop after tests.
You can migrate container via `@TestcontainersCassandra#migration` annotation parameter or manually using `CassandraConnection`.

```java
@TestcontainersMariaDB
class ExampleTests {

    @Test
    void test(@ConnectionCassandra CassandraConnection connection) {
      connection.migrationEngine(Migration.Engines.SCRIPTS).apply("migration/setup.cql");
      connection.execute("INSERT INTO users VALUES(1);");
      connection.migrationEngine(Migration.Engines.SCRIPTS).drop("migration/setup.cql", Migration.DropMode.TRUNCATE);
    }
}
```

Keyspace is created automatically.

It is recommended to **always** use construction `CREATE IF NOT EXISTS` for migration scripts, 
cause migration drop when using `TRUNCATE TABLE` on all tables in keyspace is a lot faster compared to using `DROP TABLE`.

Default strategy is to use `TRUNCATE`, if you want to change it use `Migration.DropMode.DROP`.

## Annotation

Library provides annotation based approach for creating container.

`@TestcontainersCassandra` - allow **automatically start container** with specified image in different modes without the need to configure it.

Available containers modes:

- `PER_RUN` - start container one time per *test execution*. (Containers must have same instance, e.g. compare by `==`)
- `PER_CLASS` - start new container each *test class*.
- `PER_METHOD` - start new container each *test method*.

Simple example on how to start container per class, **no need to configure** container:
```java
@TestcontainersCassandra(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @Test
    void test(@ConnectionCassandra CassandraConnection connection) {
        assertNotNull(connection);
    }
}
```

**That's all** you need.

It is possible to customize image with annotation `image` parameter.

Image also can be provided from environment variable:
```java
@TestcontainersCassandra(image = "${MY_IMAGE_ENV|cassandra:4.1}")
class ExampleTests {

    @Test
    void test() {
        // test
    }
}
```

Image syntax:

- Image can have static value: `cassandra:4.1`
- Image can be provided via environment variable using syntax: `${MY_IMAGE_ENV}`
- Image environment variable can have default value if empty using syntax: `${MY_IMAGE_ENV|cassandra:4.1}`

### Manual Container

When you need to **manually configure container** with specific options, you can provide such container as instance that will be used by `@TestcontainersCassandra`,
this can be done using `@ContainerCassandra` annotation for container.

Example:
```java
@TestcontainersCassandra(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @ContainerCassandra
    private static final CassandraContainer<?> container = new CassandraContainer<>()
            .withEnv("CASSANDRA_DC", "mydc");
    
    @Test
    void test(@ConnectionCassandra CassandraConnection connection) {
      assertEquals("mydc", connection.params().datacenter());
      assertEquals("cassandra", connection.params().keyspace());
    }
}
```

### Network

In case you want to enable [Network.SHARED](https://java.testcontainers.org/features/networking/) for containers you can do this using `network` & `shared` parameter in annotation:
```java
@TestcontainersCassandra(network = @Network(shared = true))
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
@TestcontainersCassandra(network = @Network(alias = "${MY_ALIAS_ENV|my_default_alias}"))
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

`CassandraConnection` - can be injected to field or method parameter and used to communicate with running container via `@ConnectionCassandra` annotation.
`CassandraConnection` provides connection parameters, useful asserts, checks, etc. for easier testing.

Example:
```java
@TestcontainersCassandra(mode = ContainerMode.PER_CLASS, image = "cassandra:4.1")
class ExampleTests {

    @ConnectionCassandra
    private CassandraConnection connection;

    @Test
    void test() {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO users(id) VALUES(2);");
        var usersFound = connection.queryMany("SELECT * FROM users;", r -> r.getInt(0));
        assertEquals(2, usersFound.size());
    }
}
```

### Annotation Migration

`@Migrations` allow easily migrate database between test executions and drop after tests.

Annotation parameters:
- `engine` - to use for migration.
- `apply` - parameter configures migration mode.
- `drop` - configures when to reset/drop/clear database.
- `dropMode` - configures what strategy to use for migration drop (`TRUNCATE` or `DROP`)
- `locations` - configures locations where migrations are placed.

Keyspace is created automatically.

It is recommended to **always** use construction `CREATE IF NOT EXISTS` for migration scripts,
cause migration drop when using `TRUNCATE TABLE` on all tables in keyspace is a lot faster compared to using `DROP TABLE`.

Default strategy is to use `TRUNCATE`, if you want to change it use `Migration.DropMode.DROP`.

Available migration engines:
- Scripts - For `apply` load scripts from specified paths or directories and execute in ASC order, for `drop` clean all Non System tables in all cassandra
- [Cognitor](https://github.com/patka/cassandra-migration) - For `apply` uses Cognitor Cassandra migration library, for `drop` clean all Non System tables in all cassandra

Given engine is Scripts and migration file named `1_setup.sql` is in resource directory `migration`:
```sql
CREATE TABLE IF NOT EXISTS users
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
                dropMode = Migration.DropMode.TRUNCATE,
                migrations = { "migration" }
        ))
class ExampleTests {

    @Test
    void test(@ConnectionCassandra CassandraConnection connection) {
        connection.execute("INSERT INTO cassandra.users(id) VALUES(1);");
        connection.execute("INSERT INTO users(id) VALUES(2);");
        var usersFound = connection.queryMany("SELECT * FROM users;", r -> r.getInt(0));
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
- `EXTERNAL_TEST_CASSANDRA_KEYSPACE` - Cassandra keyspace (`cassandra` by default).
- 
## License

This project licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.
