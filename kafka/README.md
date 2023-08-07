# Testcontainers Extensions Kafka

[![Minimum required Java version](https://img.shields.io/badge/Java-11%2B-blue?logo=openjdk)](https://openjdk.org/projects/jdk/11/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.goodforgod/testcontainers-extensions-kafka/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.goodforgod/testcontainers-extensions-kafka)
[![GitHub Action](https://github.com/goodforgod/testcontainers-extensions/workflows/Release/badge.svg)](https://github.com/GoodforGod/testcontainers-extensions/actions?query=workflow%3A%22Java+CI%22)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=coverage)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=GoodforGod_testcontainers-extensions&metric=ncloc)](https://sonarcloud.io/dashboard?id=GoodforGod_testcontainers-extensions)

Testcontainers Kafka Extension with advanced testing capabilities.

Features:
- Container easy run *per method*, *per class*, *per execution*.
- Kafka Producer for easy testing.
- Kafka Consumer for easy testing.

## Dependency :rocket:

**Gradle**
```groovy
testImplementation "io.goodforgod:testcontainers-extensions-kafka:0.4.2"
```

**Maven**
```xml
<dependency>
    <groupId>io.goodforgod</groupId>
    <artifactId>testcontainers-extensions-kafka</artifactId>
    <version>0.4.2</version>
    <scope>test</scope>
</dependency>
```

### Kafka Client
[Kafka Client](https://mvnrepository.com/artifact/org.apache.kafka/kafka-clients) must be on classpath, if it is somehow not on your classpath already,
don't forget to add:

**Gradle**
```groovy
testRuntimeOnly "org.apache.kafka:kafka-clients:3.5.0"
```

**Maven**
```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.5.0</version>
    <scope>test</scope>
</dependency>
```

### Kafka Client
[Kafka Client](https://mvnrepository.com/artifact/org.apache.kafka/kafka-clients) must be on classpath, if it is somehow not on your classpath already,
don't forget to add:

**Gradle**
```groovy
testRuntimeOnly "org.apache.kafka:kafka-clients:3.5.0"
```

**Maven**
```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.5.0</version>
    <scope>test</scope>
</dependency>
```

## Content
- [Container](#container)
  - [Preconfigured container](#preconfigured-container)
  - [Setup topics](#setup-topics)
- [Connection](#connection)
  - [Producer](#producer)
  - [Consumer](#consumer)
  - [Properties](#properties)
  - [External Connection](#external-connection)

## Container

`@TestcontainersKafka` - provides container start in different modes per test class.

Available containers modes:
- `PER_RUN` - start container one time per *test execution*. (Containers should have same image to be reused between test classes)
- `PER_CLASS` - start new container each *test class*.
- `PER_METHOD` - start new container each *test method*.

Simple example on how to start container per class:
```java
@TestcontainersKafka(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @Test
    void test() {
        // test
    }
}
```

It is possible to customize image with annotation `image` parameter.

### Preconfigured container

Container instance can be used by extensions via `@ContainerKafka` annotation.

Example:
```java
@TestcontainersKafka(mode = ContainerMode.PER_CLASS)
class ExampleTests {

    @ContainerKafka
    private static final KafkaContainer container = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.1"))
          .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(KafkaContainer.class)))
          .withNetwork(Network.SHARED);

    @Test
    void checkParams(@ContainerKafkaConnection KafkaConnection connection) {
        assertNotNull(connection.params().boostrapServers());
        assertNotNull(connection.params().properties());
    }
}
```

### Setup topics

It is possible configure topics for creation right after Kafka container started, such topics will be created if not exist.
This can be useful in tests before tested application started and connected to Kafka, especially with Consumers.

Example:
```java
@TestcontainersKafka(mode = ContainerMode.PER_CLASS, topics = {"my-topic"})
class ExampleTests {

}
```

## Connection

`KafkaConnection` - can be injected to field or method parameter and used to communicate with running container via `@ContainerKafkaConnection` annotation.
`KafkaConnection` provides kafka properties, ability to send events to kafka or consume events from kafka for easier testing.

### Producer

You can easily send events to any topic (if topic not exist before sending, it will be automatically created).

Example:
```java
@TestcontainersKafka(mode = ContainerMode.PER_CLASS, image = "confluentinc/cp-kafka:7.4.1")
class ExampleTests {

    @ContainerKafkaConnection
    private KafkaConnection connection;

    @Test
    void test() {
        connection.send("my-topic-name", Event.ofValue("{\"name\":\"User\"}"));
    }
}
```

### Consumer

You can easily subscribe and consume events from any topic (if topic not exist before subscribing, it will be automatically created).

Example:
```java
@TestcontainersKafka(mode = ContainerMode.PER_CLASS, image = "confluentinc/cp-kafka:7.4.1")
class ExampleTests {

    @ContainerKafkaConnection
    private KafkaConnection connection;

    @Test
    void test() {
        // given
        var consumer = connection.subscribe("my-topic-name");
        
        // when
        connection.send("my-topic-name", Event.ofValue("value1"), Event.ofValue("value2"));
        
        // then
        consumer.assertReceivedAtLeast(2, Duration.ofSeconds(5));
    }
}
```

### Properties

It is possible to provide custom properties to `@KafkaConnection` that will be applied to Produces and Consumers that are created during tests.

```java
```java
@TestcontainersKafka(mode = ContainerMode.PER_CLASS, image = "confluentinc/cp-kafka:7.4.1")
class ExampleTests {

    @ContainerKafkaConnection(properties = { @ContainerKafkaConnection.Property(name = "enable.auto.commit", value = "true") })
    private KafkaConnection connection;
}
```

### External Connection

In case you want to use some external Kafka instance that is running in CI or other place for tests (due to docker limitations or other), 
you can use special *environment variables* and extension will use them to propagate connection and no Kafka containers will be running in such case.

Special environment variables:
- `EXTERNAL_TEST_KAFKA_BOOTSTRAP_SERVERS` - Kafka instance boostrap server.
- Prefix `EXTERNAL_TEST_KAFKA_` - any environmental variable with prefix `EXTERNAL_TEST_KAFKA_` will be converted and used for `KafkaConnection`.

Prefix `EXTERNAL_TEST_KAFKA_` conversion rules: Cut prefix and lower case and replace `_` with `.`.
Example if found env `EXTERNAL_TEST_KAFKA_AUTO_OFFSET_RESET` it will be converted to `auto.offset.reset`.

## License

This project licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.
