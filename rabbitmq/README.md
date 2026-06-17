# Testcontainers Extensions RabbitMQ

[![Minimum required Java version](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Maven Central](https://img.shields.io/maven-central/v/io.goodforgod/testcontainers-extensions-rabbitmq.svg)](https://central.sonatype.com/artifact/io.goodforgod/testcontainers-extensions-rabbitmq)

Testcontainers RabbitMQ Extension with advanced testing capabilities.

Features:
- Container easy run per method, per class, per execution.
- RabbitMQ producer helpers for tests.
- RabbitMQ consumer helpers with assert and check API.
- Topology setup for queues, exchanges and bindings.

## Dependency

**Gradle**
```groovy
testImplementation "io.goodforgod:testcontainers-extensions-rabbitmq:0.14.0"
```

**Maven**
```xml
<dependency>
    <groupId>io.goodforgod</groupId>
    <artifactId>testcontainers-extensions-rabbitmq</artifactId>
    <version>0.14.0</version>
    <scope>test</scope>
</dependency>
```

### RabbitMQ Client

RabbitMQ Java client must be on classpath.

**Gradle**
```groovy
testImplementation "com.rabbitmq:amqp-client:5.21.0"
```

## Usage

```java
@TestcontainersRabbitMQ(mode = ContainerMode.PER_CLASS,
        topology = @Topology(
                queues = @Queue(name = "orders"),
                exchanges = @Exchange(name = "events", type = Exchange.Type.DIRECT),
                bindings = @Binding(queue = "orders", exchange = "events", routingKey = "orders.created")))
class ExampleTests {

    @ConnectionRabbitMQ
    private RabbitMQConnection connection;

    @Test
    void test() {
        var consumer = connection.subscribe("orders");
        connection.send("events", "orders.created", Event.ofValue("value1"), Event.ofValue("value2"));
        consumer.assertReceivedAtLeast(2, Duration.ofSeconds(5));
    }
}
```

## Connection

`RabbitMQConnection` can be injected to a field or method parameter and used to publish messages,
subscribe queues, assert received messages and declare topology.

## Annotation

`@TestcontainersRabbitMQ` automatically starts RabbitMQ with the specified image in different modes.
It also supports declaring topology before tests run.

Available container modes:
- `PER_RUN`
- `PER_CLASS`
- `PER_METHOD`

## Topology

Topology can be configured directly in annotation:

```java
@TestcontainersRabbitMQ(
        topology = @Topology(
                queues = @Queue(name = "orders"),
                exchanges = @Exchange(name = "events", type = Exchange.Type.DIRECT),
                bindings = @Binding(queue = "orders", exchange = "events", routingKey = "orders.created")))
class ExampleTests {}
```

Topology reset modes:
- `NONE`
- `PER_CLASS`
- `PER_METHOD`

## External Connection

If you want to use an already running RabbitMQ instance, the extension supports:
- `EXTERNAL_TEST_RABBITMQ_URI`
- `EXTERNAL_TEST_RABBITMQ_*`

Prefix conversion rules: cut prefix, lower-case and replace `_` with `.`.

## License

This project licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.