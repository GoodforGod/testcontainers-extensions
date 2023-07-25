package io.goodforgod.testcontainers.extensions.redis;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

@TestcontainersRedis(mode = ContainerMode.PER_CLASS, image = "redis:7.0-alpine")
class RedisConnectionAssertsTests {

    @ContainerRedisConnection
    private RedisConnection connection;

    @BeforeEach
    void clean() {
        connection.deleteAll();
    }

    @Test
    void countPrefix() {
        connection.commands().set("11", "1");
        connection.commands().set("12", "2");
        assertEquals(2, connection.countPrefix(RedisKey.of("1")));
    }

    @Test
    void assertCountsPrefixNoneWhenMore() {
        connection.commands().set("11", "1");
        connection.commands().set("12", "2");
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsPrefixNone(RedisKey.of("1")));
    }

    @Test
    void assertCountsPrefixNoneWhenZero() {
        assertDoesNotThrow(() -> connection.assertCountsPrefixNone(RedisKey.of("1")));
    }

    @Test
    void assertCountsPrefixAtLeastWhenZero() {
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsPrefixAtLeast(1, RedisKey.of("1")));
    }

    @Test
    void assertCountsPrefixAtLeastWhenMore() {
        connection.commands().set("11", "1");
        connection.commands().set("12", "2");
        assertDoesNotThrow(() -> connection.assertCountsPrefixAtLeast(1, RedisKey.of("1")));
    }

    @Test
    void assertCountsPrefixAtLeastWhenEquals() {
        connection.commands().set("11", "1");
        assertDoesNotThrow(() -> connection.assertCountsPrefixAtLeast(1, RedisKey.of("1")));
    }

    @Test
    void assertCountsPrefixExactWhenZero() {
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsPrefixEquals(1, RedisKey.of("1")));
    }

    @Test
    void count() {
        connection.commands().set("11", "1");
        connection.commands().set("12", "2");
        assertEquals(1, connection.count(RedisKey.of("11")));
    }

    @Test
    void assertCountsNoneWhenMore() {
        connection.commands().set("11", "1");
        connection.commands().set("12", "2");
        var k1 = RedisKey.of("11");
        var k2 = RedisKey.of("12");
        assertNotEquals(k1, k2);
        assertNotEquals(k1.toString(), k2.toString());
        assertNotEquals(k1.hashCode(), k2.hashCode());
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsNone(List.of(k1, k2)));
    }

    @Test
    void assertCountsNoneWhenZero() {
        assertDoesNotThrow(() -> connection.assertCountsNone(RedisKey.of("1")));
    }

    @Test
    void assertCountsAtLeastWhenZero() {
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsAtLeast(1, RedisKey.of("1")));
    }

    @Test
    void assertCountsAtLeastWhenOther() {
        connection.commands().set("11", "{\"a\":1}");
        connection.commands().set("12", "{\"a\":2}");
        assertDoesNotThrow(() -> connection.assertCountsAtLeast(1, RedisKey.of("11", "22")));
    }

    @Test
    void assertCountsAtLeastWhenMore() {
        connection.commands().set("11", "{\"a\":1}");
        connection.commands().set("12", "{\"a\":2}");
        var values = assertDoesNotThrow(() -> connection.assertCountsAtLeast(1, RedisKey.of("11", "12")));
        assertEquals(2, values.size());
        assertNotEquals(values.get(0), values.get(1));
        assertNotEquals(values.get(0).hashCode(), values.get(1).hashCode());
        assertNotEquals(values.get(0).toString(), values.get(1).toString());
        assertNotEquals(values.get(0).asJson(), values.get(1).asJson());
        assertNotEquals(values.get(0).asJson().toString(), values.get(1).asJson().toString());
    }

    @Test
    void assertCountsAtLeastWhenEquals() {
        connection.commands().set("11", "1");
        assertDoesNotThrow(() -> connection.assertCountsAtLeast(1, RedisKey.of("11")));
    }

    @Test
    void assertCountsExactWhenZero() {
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsEquals(1, RedisKey.of("1")));
    }
}
