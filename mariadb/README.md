# Testcontainers Extensions MariaDB

[![Minimum required Java version](https://img.shields.io/badge/Java-11%2B-blue?logo=openjdk)](https://openjdk.org/projects/jdk/11/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.goodforgod/testcontainers-extensions-mariadb/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.goodforgod/testcontainers-extensions-mariadb)
[![GitHub Action](https://github.com/goodforgod/testcontainers-extensions/workflows/Release/badge.svg)](https://github.com/GoodforGod/testcontainers-extensions/actions?query=workflow%3A"CI+Master"++)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=coverage)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=ncloc)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)

Testcontainers MariaDB Extension with advanced testing capabilities.

Features:
- Container easy run *per method*, *per class*, *per execution*.
- Container easy migration with *[Flyway](https://documentation.red-gate.com/fd/mariadb-184127600.html)* / *[Liquibase](https://www.liquibase.com/databases/mariadb-server)*.
- Container easy connection injection with asserts.

## Dependency :rocket:

**Gradle**
```groovy
testImplementation "io.goodforgod:testcontainers-extensions-mariadb:0.7.0"
```

**Maven**
```xml
<dependency>
    <groupId>io.goodforgod</groupId>
    <artifactId>testcontainers-extensions-mariadb</artifactId>
    <version>0.7.0</version>
    <scope>test</scope>
</dependency>
```

### JDBC Driver
[MariaDB JDBC Driver](https://mvnrepository.com/artifact/org.mariadb.jdbc/mariadb-java-client) must be on classpath, if it is somehow not on your classpath already,
don't forget to add:

**Gradle**
```groovy
testRuntimeOnly "org.mariadb.jdbc:mariadb-java-client:3.1.4"
```

**Maven**
```xml
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
    <version>3.1.4</version>
    <scope>test</scope>
</dependency>
```

## Content
- [Container](#container)
  - [Manual Container](#manual-container)
- [Connection](#connection)
  - [External Connection](#external-connection)
- [Migration](#migration)

## Container

`@TestcontainersMariadb` - allow **automatically start container** with specified image in different modes without the need to configure it.

Available containers modes:

- `PER_RUN` - start container one time per *test execution*. (Containers must have same instance, e.g. compare by `==`)
- `PER_CLASS` - start new container each *test class*.
- `PER_METHOD` - start new container each *test method*.

Simple example on how to start container per class, **no need to configure** container:
```java
@TestcontainersMariadb(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @Test
    void test(@ContainerMariadbConnection JdbcConnection connection) {
        assertNotNull(connection);
    }
}
```

**That's all** you need.

It is possible to customize image with annotation `image` parameter.

Image also can be provided from environment variable:
```java
@TestcontainersMariadb(image = "${MY_IMAGE_ENV|mariadb:11.0-jammy}")
class ExampleTests {

    @Test
    void test() {
        // test
    }
}
```

Image syntax:

- Image can have static value: `mariadb:11.0-jammy`
- Image can be provided via environment variable using syntax: `${MY_IMAGE_ENV}`
- Image environment variable can have default value if empty using syntax: `${MY_IMAGE_ENV|mariadb:11.0-jammy}`

### Manual Container

When you need to **manually configure container** with specific options, you can provide such container as instance that will be used by `@TestcontainersMariadb`,
this can be done using `@ContainerMariadb` annotation for container.

Example:
```java
@TestcontainersMariadb(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @ContainerMariadb
    private static final MariaDBContainer<?> container = new MariaDBContainer<>()
            .withDatabaseName("user")
            .withUsername("user")
            .withPassword("user");
    
    @Test
    void test(@ContainerMariadbConnection JdbcConnection connection) {
        assertEquals("user", connection.params().database());
        assertEquals("user", connection.params().username());
        assertEquals("user", connection.params().password());
    }
}
```

### Network

In case you want to enable [Network.SHARED](https://java.testcontainers.org/features/networking/) for containers you can do this using `network` & `shared` parameter in annotation:
```java
@TestcontainersMariadb(network = @Network(shared = true))
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
@TestcontainersMariadb(network = @Network(alias = "${MY_ALIAS_ENV|my_default_alias}"))
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

## Connection

`JdbcConnection` - can be injected to field or method parameter and used to communicate with running container via `@ContainerMariadbConnection` annotation.
`JdbcConnection` provides connection parameters, useful asserts, checks, etc. for easier testing.

Example:
```java
@TestcontainersMariadb(mode = ContainerMode.PER_CLASS, image = "mariadb:11.0-jammy")
class ExampleTests {

    @ContainerMariadbConnection
    private JdbcConnection connectionInField;

    @Test
    void test(@ContainerMariadbConnection JdbcConnection connection) {
        connection.execute("CREATE TABLE users (id INT NOT NULL PRIMARY KEY);");
        connection.execute("INSERT INTO users VALUES(1);");
        connection.assertInserted("INSERT INTO users VALUES(2);");
        var usersFound = connection.queryMany("SELECT * FROM users;", r -> r.getInt(1));
        assertEquals(2, usersFound.size());
        connection.assertQueriesEquals(2, "SELECT * FROM users;");
    }
}
```

### External Connection

In case you want to use some external Mariadb instance that is running in CI or other place for tests (due to docker limitations or other), 
you can use special *environment variables* and extension will use them to propagate connection and no Mariadb containers will be running in such case.

Special environment variables:
- `EXTERNAL_TEST_MARIADB_JDBC_URL` - Mariadb instance JDBC url.
- `EXTERNAL_TEST_MARIADB_USERNAME` - Mariadb instance username (optional).
- `EXTERNAL_TEST_MARIADB_PASSWORD` - Mariadb instance password (optional).
- `EXTERNAL_TEST_MARIADB_HOST` - Mariadb instance host (optional if JDBC url specified).
- `EXTERNAL_TEST_MARIADB_PORT` - Mariadb instance port (optional if JDBC url specified).
- `EXTERNAL_TEST_MARIADB_DATABASE` - Mariadb instance database (`mariadb` by default) (optional if JDBC url specified)

Use can use either `EXTERNAL_TEST_MARIADB_JDBC_URL` to specify connection with username & password combination
or use combination of `EXTERNAL_TEST_MARIADB_HOST` & `EXTERNAL_TEST_MARIADB_PORT` & `EXTERNAL_TEST_MARIADB_DATABASE`.

`EXTERNAL_TEST_MARIADB_JDBC_URL` env have higher priority over host & port & database.

## Migration

`@Migrations` allow easily migrate database between test executions and drop after tests.

Annotation parameters:
- `engine` - to use for migration.
- `apply` - parameter configures migration mode.
- `drop` - configures when to reset/drop/clear database.

Available migration engines:
- [Flyway](https://documentation.red-gate.com/fd/mariadb-184127600.html)
- [Liquibase](https://www.liquibase.com/databases/mariadb-server)

Given engine is [Flyway](https://documentation.red-gate.com/fd/mariadb-184127600.html) and migration file named `V1__flyway.sql` is in resource directory on default path `db/migration`:
```sql
CREATE TABLE IF NOT EXISTS users
(
    id INT NOT NULL PRIMARY KEY
);
```

Test with container and migration per method will look like:
```java
@TestcontainersMariadb(mode = ContainerMode.PER_CLASS,
        migration = @Migration(
                engine = Migration.Engines.FLYWAY,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD))
class ExampleTests {

    @Test
    void test(@ContainerMariadbConnection JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        var usersFound = connection.queryMany("SELECT * FROM users;", r -> r.getInt(1));
        assertEquals(1, usersFound.size());
    }
}
```

## License

This project licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.
