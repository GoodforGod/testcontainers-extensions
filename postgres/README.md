# Testcontainers Extensions Postgres

[![GitHub Action](https://github.com/goodforgod/testcontainers-extensions-postgres/workflows/Java%20CI/badge.svg)](https://github.com/GoodforGod/testcontainers-extensions-postgres/actions?query=workflow%3A%22Java+CI%22)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions-postgres&metric=coverage)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions-postgres)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions-postgres&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions-postgres)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions-postgres&metric=ncloc)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions-postgres)

Testcontainers Postgres Extension with advanced testing features.

Features:
- Container easy run *per method*, *per class*, *per execution*.
- Container easy migration with *[Flyway](https://documentation.red-gate.com/fd/quickstart-how-flyway-works-184127223.html)* / *[Liquibase](https://docs.liquibase.com/concepts/introduction-to-liquibase.html)*.
- Container easy connection injection with asserts.

## Dependency :rocket:

**Gradle**
```groovy
testImplementation "io.goodforgod:testcontainers-extensions-postgres:0.1.0"
```

**Maven**
```xml
<dependency>
    <groupId>io.goodforgod</groupId>
    <artifactId>testcontainers-extensions-postgres</artifactId>
    <version>0.1.0</version>
    <scope>test</scope>
</dependency>
```

## Usage

`@TestcontainersJdbc` - provides container start in different modes per test class.

Available containers modes:
- `PER_RUN` - start container one time per test execution. (Containers with mode `PER_RUN` should have same image to be reused between test classes)
- `PER_CLASS` - start new container each test class.
- `PER_METHOD` - start new container each test method.

Simple example on how to start container per class:
```java
@TestcontainersJdbc(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @Test
    void test(@ContainerJdbcConnection JdbcConnection connection) {
        assertNotNull(connection);
    }
}
```

#### Pre configured instance

Container instance can be used by extensions via `@ContainerJdbc` annotation.

Example:
```java
@TestcontainersJdbc(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @ContainerJdbc
    private static final PostgreSQLContainer<?> container = new PostgreSQLContainer<>()
            .withDatabaseName("user")
            .withUsername("user")
            .withPassword("user");
    
    @Test
    void test(@ContainerJdbcConnection JdbcConnection connection) {
        assertEquals("user", connection.params().database());
        assertEquals("user", connection.params().username());
        assertEquals("user", connection.params().password());
    }
}
```

### Connection

`JdbcConnection` - can be injected to field or method parameter and used to communicate with running container via `@ContainerJdbcConnection` annotation.
`JdbcConnection` provides connection parameters, useful asserts, checks, etc.

Example:
```java
@TestcontainersJdbc(mode = ContainerMode.PER_CLASS, image = "postgres:15.2-alpine")
class ExampleTests {

    @ContainerJdbcConnection
    private JdbcConnection connectionInField;

    @Test
    void test(@ContainerJdbcConnection JdbcConnection connectionInParam) {
        connection.execute("CREATE TABLE users (id INT NOT NULL PRIMARY KEY);");
        connection.execute("INSERT INTO users VALUES(1);");
        connection.execute("INSERT INTO users VALUES(2);");
        var usersFound = connection.queryMany("SELECT * FROM users;", r -> r.getInt(1));
        assertEquals(2, usersFound.size());
    }
}
```

### Migration

`@Migrations` allow easily migrate database between test executions and drop after tests.
`apply` parameter configures migration mode and `drop` configures when to reset/drop/clear database.

Available migration engines:
- [Flyway](https://documentation.red-gate.com/fd/quickstart-how-flyway-works-184127223.html)
- [Liquibase](https://docs.liquibase.com/concepts/introduction-to-liquibase.html)

Given such migration for [Flyway](https://documentation.red-gate.com/fd/quickstart-how-flyway-works-184127223.html) on default path `db/migration`:
```sql
CREATE TABLE IF NOT EXISTS users
(
    id INT NOT NULL PRIMARY KEY
);
```

Test with container and migration per method will look like:
```java
@TestcontainersJdbc(mode = ContainerMode.PER_CLASS,
        migration = @Migration(
                engine = Migration.Engines.FLYWAY,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD))
class ExampleTests {

    @Test
    void test(@ContainerJdbcConnection JdbcConnection connectionInParam) {
        connection.execute("INSERT INTO users VALUES(1);");
        var usersFound = connection.queryMany("SELECT * FROM users;", r -> r.getInt(1));
        assertEquals(1, usersFound.size());
    }
}
```

## License

This project licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.
