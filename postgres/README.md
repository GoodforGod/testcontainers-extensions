# Testcontainers Extensions Postgres

[![Minimum required Java version](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Maven Central](https://img.shields.io/maven-central/v/io.goodforgod/testcontainers-extensions-postgres.svg)](https://central.sonatype.com/artifact/io.goodforgod/testcontainers-extensions-postgres)
[![GitHub Action](https://github.com/goodforgod/testcontainers-extensions/workflows/Release/badge.svg)](https://github.com/GoodforGod/testcontainers-extensions/actions?query=workflow%3A"CI+Master"++)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=coverage)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=ncloc)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)

Testcontainers Postgres Extension with advanced testing capabilities.

Features:
- Container easy run *per method*, *per class*, *per execution*.
- Container easy migration with *[Flyway](https://documentation.red-gate.com/fd/postgresql-184127604.html)* / *[Liquibase](https://www.liquibase.com/databases/postgresql)*.
- Container easy connection injection with asserts.

## Dependency :rocket:

**Gradle**
```groovy
testImplementation "io.goodforgod:testcontainers-extensions-postgres:0.16.0"
```

**Maven**
```xml
<dependency>
    <groupId>io.goodforgod</groupId>
    <artifactId>testcontainers-extensions-postgres</artifactId>
    <version>0.16.0</version>
    <scope>test</scope>
</dependency>
```

### JDBC Driver
[Postgres JDBC Driver](https://mvnrepository.com/artifact/org.postgresql/postgresql) must be on classpath, if it is somehow not on your classpath already,
don't forget to add:

**Gradle**
```groovy
testRuntimeOnly "org.postgresql:postgresql:42.6.0"
```

**Maven**
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.6.0</version>
    <scope>test</scope>
</dependency>
```

## Content
- [Usage](#usage)
- [Connection](#connection)
  - [Migration](#connection-migration)
- [Annotation](#annotation)
  - [Manual Container](#manual-container)
  - [Connection](#annotation-connection)
  - [Isolation](#isolation)
  - [External Connection](#external-connection)
  - [Migration](#annotation-migration)
  - [Migration Strategy](#migration-strategy)
  - [Parallel Tests & Template Cloning](#parallel-tests--template-cloning-deep-dive)
  - [Migration Strategy](#migration-strategy)

## Usage

Test with container start in `PER_RUN` mode and migration per method will look like:

```java
@TestcontainersPostgreSQL(mode = ContainerMode.PER_RUN,
        migration = @Migration(
                engine = Migration.Engines.FLYWAY,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD))
class ExampleTests {

  @ConnectionPostgreSQL
  private JdbcConnection connection;

  @Test
  void test() {
    connection.execute("INSERT INTO users VALUES(1);");
    var usersFound = connection.queryMany("SELECT * FROM users;", r -> r.getInt(1));
    assertEquals(1, usersFound.size());
  }
}
```

## Connection

`JdbcConnection` is an abstraction with asserting data in database container and easily manipulate container connection settings.
You can inject connection via `@ConnectionPostgreSQL` as field or method argument or manually create it from container or manual settings.

```java
class ExampleTests {

    private static final PostgreSQLContainer<?> container = new PostgreSQLContainer<>();
    
    @Test
    void test() {
      container.start();
      JdbcConnection connection = JdbcConnection.forContainer(container);
      connection.execute("INSERT INTO users VALUES(1);");
    }
}
```

### Connection Migration

`Migrations` allow easily migrate database between test executions and drop after tests.
You can migrate container via `@TestcontainersPostgreSQL#migration` annotation parameter or manually using `JdbcConnection`.

```java
@TestcontainersPostgreSQL
class ExampleTests {

    @Test
    void test(@ConnectionPostgreSQL JdbcConnection connection) {
      connection.migrationEngine(Migration.Engines.FLYWAY).apply("db/migration");
      connection.execute("INSERT INTO users VALUES(1);");
      connection.migrationEngine(Migration.Engines.FLYWAY).drop("db/migration");
    }
}
```

Available migration engines:
- [Flyway](https://documentation.red-gate.com/fd/cockroachdb-184127591.html)
- [Liquibase](https://www.liquibase.com/databases/cockroachdb-2)

## Annotation

`@TestcontainersPostgreSQL` - allow **automatically start container** with specified image in different modes without the need to configure it.

Available containers modes:

- `PER_RUN` - start container one time per *test execution*. (Containers must have same instance, e.g. compare by `==`)
- `PER_CLASS` - start new container each *test class*.
- `PER_METHOD` - start new container each *test method*.

Simple example on how to start container per class, **no need to configure** container:
```java
@TestcontainersPostgreSQL(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @Test
    void test(@ConnectionPostgreSQL JdbcConnection connection) {
        assertNotNull(connection);
    }
}
```

**That's all** you need.

It is possible to customize image with annotation `image` parameter.

Image also can be provided from environment variable:
```java
@TestcontainersPostgreSQL(image = "${MY_IMAGE_ENV|postgres:18.6-alpine}")
class ExampleTests {

    @Test
    void test() {
        // test
    }
}
```

Image syntax:

- Image can have static value: `postgres:18.6-alpine`
- Image can be provided via environment variable using syntax: `${MY_IMAGE_ENV}`
- Image environment variable can have default value if empty using syntax: `${MY_IMAGE_ENV|postgres:18.6-alpine}`

### Manual Container

When you need to **manually configure container** with specific options, you can provide such container as instance that will be used by `@TestcontainersPostgreSQL`,
this can be done using `@ContainerPostgreSQL` annotation for container.

```java
@TestcontainersPostgreSQL(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @ContainerPostgreSQL
    private static final PostgreSQLContainer<?> container = new PostgreSQLContainer<>()
            .withDatabaseName("user")
            .withUsername("user")
            .withPassword("user");
    
    @Test
    void test(@ConnectionPostgreSQL JdbcConnection connection) {
        assertEquals("user", connection.params().database());
        assertEquals("user", connection.params().username());
        assertEquals("user", connection.params().password());
    }
}
```

### Network

In case you want to enable [Network.SHARED](https://java.testcontainers.org/features/networking/) for containers you can do this using `network` & `shared` parameter in annotation:
```java
@TestcontainersPostgreSQL(network = @Network(shared = true))
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
@TestcontainersPostgreSQL(network = @Network(alias = "${MY_ALIAS_ENV|my_default_alias}"))
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

`JdbcConnection` - can be injected to field or method parameter and used to communicate with running container via `@ConnectionPostgreSQL` annotation.
`JdbcConnection` provides connection parameters, useful asserts, checks, etc. for easier testing.

```java
@TestcontainersPostgreSQL(mode = ContainerMode.PER_CLASS, image = "postgres:18.6-alpine")
class ExampleTests {

    @ConnectionPostgreSQL
    private JdbcConnection connection;

    @Test
    void test() {
        connection.execute("CREATE TABLE users (id INT NOT NULL PRIMARY KEY);");
        connection.execute("INSERT INTO users VALUES(1);");
        connection.assertInserted("INSERT INTO users VALUES(2);");
        var usersFound = connection.queryMany("SELECT * FROM users;", r -> r.getInt(1));
        assertEquals(2, usersFound.size());
        connection.assertQueriesEquals(2, "SELECT * FROM users;");
    }
}
```

### Isolation

`Isolation` controls logical connection isolation inside a started container.

Default value is `@Isolation(Isolation.Mode.DISABLED)`. Disabled isolation preserves regular behavior: injected connection points to the container database and migrations run directly against that database according to `Migration.Mode`.

`Isolation.Mode.PER_METHOD` reuses the same physical container but creates a separate generated database for every test method. Field and method argument injection receive a `JdbcConnection` configured for that generated database.

`Isolation.Mode.PER_METHOD` has lifecycle restrictions:
- Field injection requires JUnit default `TestInstance.Lifecycle.PER_METHOD`.
- `TestInstance.Lifecycle.PER_CLASS` is rejected.
- Constructor injection is rejected.
- `@BeforeAll` parameter injection is rejected.

```java
@TestcontainersPostgreSQL(mode = ContainerMode.PER_RUN,
        isolation = @Isolation(Isolation.Mode.PER_METHOD),
        migration = @Migration(
                engine = Migration.Engines.FLYWAY,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.NONE))
class ExampleTests {

    @ConnectionPostgreSQL
    private JdbcConnection connection;

    @Test
    void test(@ConnectionPostgreSQL JdbcConnection parameter) {
        assertSame(connection, parameter);
        connection.execute("INSERT INTO users VALUES(1);");
    }
}
```

### External Connection

In case you want to use some external Postgres instance that is running in CI or other place for tests (due to docker limitations or other), 
you can use special *environment variables* and extension will use them to propagate connection and no Postgres containers will be running in such case.

Special environment variables:
- `EXTERNAL_TEST_POSTGRES_JDBC_URL` - Postgres instance JDBC url.
- `EXTERNAL_TEST_POSTGRES_USERNAME` - Postgres instance username (optional).
- `EXTERNAL_TEST_POSTGRES_PASSWORD` - Postgres instance password (optional).
- `EXTERNAL_TEST_POSTGRES_HOST` - Postgres instance host (optional if JDBC url specified).
- `EXTERNAL_TEST_POSTGRES_PORT` - Postgres instance port (optional if JDBC url specified).
- `EXTERNAL_TEST_POSTGRES_DATABASE` - Postgres instance database (`postgres` by default) (optional if JDBC url specified)

Use can use either `EXTERNAL_TEST_POSTGRES_JDBC_URL` to specify connection with username & password combination
or use combination of `EXTERNAL_TEST_POSTGRES_HOST` & `EXTERNAL_TEST_POSTGRES_PORT` & `EXTERNAL_TEST_POSTGRES_DATABASE`.

`EXTERNAL_TEST_POSTGRES_JDBC_URL` env have higher priority over host & port & database.

### Annotation Migration

`@Migrations` allow easily migrate database between test executions and drop after tests.

Annotation parameters:
- `engine` - to use for migration.
- `apply` - parameter configures migration mode.
- `drop` - configures when to reset/drop/clear database.
- `locations` - configures locations where migrations are placed.
- `strategy` - configures how migrations are applied to isolated databases.

Available migration engines:
- [Flyway](https://documentation.red-gate.com/fd/postgresql-184127604.html)
- [Liquibase](https://www.liquibase.com/databases/postgresql)

Given engine is [Flyway](https://documentation.red-gate.com/fd/postgresql-184127604.html) and migration file named `V1__flyway.sql` is in resource directory on default path `db/migration`:
```sql
CREATE TABLE IF NOT EXISTS users
(
    id INT NOT NULL PRIMARY KEY
);
```

Test with container and migration per method will look like:
```java
@TestcontainersPostgreSQL(mode = ContainerMode.PER_CLASS,
        migration = @Migration(
                engine = Migration.Engines.FLYWAY,
                apply = Migration.Mode.PER_METHOD,
                drop = Migration.Mode.PER_METHOD))
class ExampleTests {

    @Test
    void test(@ConnectionPostgreSQL JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        var usersFound = connection.queryMany("SELECT * FROM users;", r -> r.getInt(1));
        assertEquals(1, usersFound.size());
    }
}
```

### Migration Strategy

`Migration.Strategy.DEFAULT` runs Flyway or Liquibase directly against the active connection. This is the default and keeps existing migration behavior.

`Migration.Strategy.TEMPLATE_CLONE` is supported by Postgres for `Isolation.Mode.PER_METHOD`. The extension creates a `migration_template_<hash>` database, applies migrations to it once, closes the template connection, and then creates every test method database using `CREATE DATABASE <generated> TEMPLATE <template>`.

This is useful when migrations are expensive and tests need isolated databases:

```java
@TestcontainersPostgreSQL(mode = ContainerMode.PER_RUN,
        isolation = @Isolation(Isolation.Mode.PER_METHOD),
        migration = @Migration(
                engine = Migration.Engines.FLYWAY,
                apply = Migration.Mode.PER_CLASS,
                drop = Migration.Mode.NONE,
                strategy = Migration.Strategy.TEMPLATE_CLONE))
class ExampleTests {

    @Test
    void test(@ConnectionPostgreSQL JdbcConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
        connection.assertCountsEquals(1, "users");
    }
}
```

`TEMPLATE_CLONE` requires `Isolation.Mode.PER_METHOD`. Using it with default disabled isolation fails fast because it would change historical migration behavior.

### Parallel Tests & Template Cloning (Deep Dive)

This section explains **why** this machinery exists, **what** each moving part is responsible for, and **how** they work together to make isolated, migrated databases cheap enough to run tests in parallel.

#### Why: the problem being solved

Integration tests that touch a database usually fight over two things:

1. **Shared state** — when every test writes to the same database, tests leak data into each other. This forces you to either clean up manually between tests or run everything sequentially, both of which are slow and fragile.
2. **Migration cost** — giving every test its own fresh database is the clean solution, but re-running Flyway/Liquibase for hundreds of test methods is expensive (each run replays the entire migration history against an empty database).

The goal is to have the isolation of "one database per test" **without** paying the migration cost "one full migration run per test", so that tests can safely run **in parallel** against a single container.

#### What: the moving parts

| Piece | Responsibility |
|-------|----------------|
| `ContainerMode.PER_RUN` | Start **one** physical Postgres container and reuse it for the whole test run. All isolated databases live inside this single container. |
| `Isolation.Mode.PER_METHOD` | Give **every test method its own logical database** inside that shared container, instead of sharing the container's default database. |
| `Migration.Strategy.TEMPLATE_CLONE` | Migrate a **template database once**, then create each per-method database as a fast physical copy of that template rather than re-running migrations. |
| Generated namespace | The unique, per-method database name (`<image-prefix>_<uuid>`) that keeps methods from colliding. |

#### How: step by step

**1. Container starts once.** With `mode = ContainerMode.PER_RUN` a single container is started and kept alive for the entire run. Every isolated database is a database inside this one server, so there is no per-test container startup cost.

**2. A migrated template is built exactly once.** The first time an isolated connection is requested, the extension computes a template key from `(provider, image, engine, migration locations)` and, under a lock, creates a database named `migration_template_<hash>`, runs the configured engine (Flyway/Liquibase) against it a single time, and closes the template connection. The result is cached, so all subsequent tests reuse the same already-migrated template. The `<hash>` is derived from the template key, so different images or migration locations get their own templates and never clash.

**3. Each test method gets its own database cloned from the template.** Before a test method runs, the extension generates a unique namespace — `<image-prefix>_<uuid>` (prefix taken from the image name, lowercased and truncated) — and issues:

```sql
CREATE DATABASE <generated_namespace> TEMPLATE migration_template_<hash>;
```

Postgres copies the template's files at the storage level. This is dramatically cheaper than replaying migrations, because the schema (and any seed data baked into the template) already exists — the database is born fully migrated.

**4. The injected connection is re-pointed to that database.** The `JdbcConnection` handed to the test (via `@ConnectionPostgreSQL` field or parameter) has its JDBC URL rewritten to target the generated database — both for the direct connection and, when a shared network is used, for the in-network params. From the test's point of view it simply gets a clean, migrated database; it never sees the template or other methods' databases.

**5. Cleanup.** The per-method connection is closed after the method finishes. Because each method used a distinct database, nothing needs to be rolled back or truncated between methods.

#### How this enables parallel execution

Because the databases are physically distinct, two test methods can run at the same time without touching each other's data. To actually run them concurrently, enable JUnit's parallel execution (this library does not force it on). For example, in `src/test/resources/junit-platform.properties`:

```properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
```

Keep the JUnit default `TestInstance.Lifecycle.PER_METHOD` — `PER_METHOD` isolation deliberately rejects `PER_CLASS` lifecycle, constructor injection, and `@BeforeAll` parameter injection, because those phases run once for many methods and could otherwise expose one method's database to another.

#### Guardrails (fail-fast validation)

- `TEMPLATE_CLONE` **requires** `Isolation.Mode.PER_METHOD`; combining it with disabled isolation fails fast, since it would silently change historical migration behavior.
- `TEMPLATE_CLONE` is only honored by providers that support it (Postgres does, via `CREATE DATABASE ... TEMPLATE ...`); unsupported providers reject it rather than degrade silently.
- `Isolation.Mode.PER_METHOD` with an incompatible test lifecycle (`PER_CLASS`, constructor injection, `@BeforeAll` injection) is rejected up front.

#### When to use which strategy

- Use `Migration.Strategy.DEFAULT` (the default) when you don't need per-method isolation, or when migrations are cheap and applied per class/run against a single database.
- Use `Migration.Strategy.TEMPLATE_CLONE` with `Isolation.Mode.PER_METHOD` + `ContainerMode.PER_RUN` when you want isolated, migrated databases per test and migrations are expensive enough that cloning a template beats re-running them — this is the configuration that unlocks fast parallel tests.

## License

This project licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.
