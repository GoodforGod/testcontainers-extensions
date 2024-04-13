# Testcontainers Extensions

[![Minimum required Java version](https://img.shields.io/badge/Java-11%2B-blue?logo=openjdk)](https://openjdk.org/projects/jdk/11/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.goodforgod/testcontainers-extensions-postgres/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.goodforgod/testcontainers-extensions-postgres)
[![GitHub Action](https://github.com/goodforgod/testcontainers-extensions/workflows/CI%20Master/badge.svg)](https://github.com/GoodforGod/testcontainers-extensions/actions?query=workflow%3A"CI+Master"++)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=coverage)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=ncloc)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)

Testcontainers Extensions with advanced testing capabilities.

Makes testing & asserts with Testcontainers even easier.

## Featured extensions
- [Postgres](postgres)
- [Kafka](kafka)
- [Oracle](oracle)
- [MariaDB](mariadb)
- [MySQL](mysql)
- [Cockroachdb](cockroachdb)
- [Cassandra](cassandra)
- [Redis](redis)
- [MockServer](mockserver)

## Usage

Here is an example of [Kafka Extension](kafka) where KafkaContainer is started in `PER_RUN` mode with topic reset per method:

```java
@TestcontainersKafka(mode = ContainerMode.PER_RUN,
        topics = @Topics(value = "my-topic-name", reset = Topics.Mode.PER_METHOD))
class ExampleTests {

  @ContainerKafkaConnection 
  private KafkaConnection connection;
  
  @Test
  void test() {
    var consumer = connection.subscribe("my-topic-name");
    connection.send("my-topic-name", Event.ofValue("event1"), Event.ofValue("event2"));
    consumer.assertReceivedAtLeast(2, Duration.ofSeconds(5));
  }
}
```

Here is an example of [Postgres Extension](postgres) where PostgresContainer is started `PER_RUN` mode and migrations are applied per method:

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