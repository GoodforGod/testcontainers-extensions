package io.goodforgod.testcontainers.extensions.redis;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

@TestcontainersRedis(mode = ContainerMode.PER_CLASS, image = "redis:7.0-alpine")
class RedisConnectionAssertsTests {

    @BeforeEach
    void clean(@ContainerRedisConnection RedisConnection connection) {
        connection.deleteAll();
    }

    @Test
    void countPrefix(@ContainerRedisConnection RedisConnection connection) {
        connection.get().sadd("11", "1");
        connection.get().sadd("12", "2");
        assertEquals(1, connection.countPrefix("1"));
    }

    @Test
    void assertCountsPrefixNoneWhenMore(@ContainerRedisConnection RedisConnection connection) {
        connection.get().sadd("11", "1");
        connection.get().sadd("12", "2");
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsPrefixNone("2"));
    }

    @Test
    void count(@ContainerRedisConnection RedisConnection connection) {
        connection.get().sadd("11", "1");
        connection.get().sadd("12", "2");
        assertEquals(1, connection.count("11"));
    }

    @Test
    void assertCountsNoneWhenMore(@ContainerRedisConnection RedisConnection connection) {
        connection.get().sadd("11", "1");
        connection.get().sadd("12", "2");
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsNone("11", "12"));
    }

    @Test
    void assertCountsNoneWhenZero(@ContainerRedisConnection RedisConnection connection) {
        assertDoesNotThrow(() -> connection.assertCountsPrefixNone("1"));
    }

    @Test
    void assertCountsAtLeastWhenZero(@ContainerRedisConnection RedisConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsPrefixAtLeast(1, "1"));
    }

    @Test
    void assertCountsAtLeastWhenMore(@ContainerRedisConnection RedisConnection connection) {
        connection.get().sadd("11", "1");
        connection.get().sadd("12", "2");
        assertDoesNotThrow(() -> connection.assertCountsPrefixAtLeast(1, "1"));
    }

    @Test
    void assertCountsAtLeastWhenEquals(@ContainerRedisConnection RedisConnection connection) {
        connection.get().sadd("11", "1");
        assertDoesNotThrow(() -> connection.assertCountsPrefixAtLeast(1, "1"));
    }

    @Test
    void assertCountsExactWhenZero(@ContainerRedisConnection RedisConnection connection) {
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsPrefixEquals(1, "1"));
    }
    //
    // @Test
    // void assertCountsExactWhenMore(@ContainerRedisConnection RedisConnection connection) {
    // connection.get().sadd("11", "1");
    // connection.get().sadd("12", "2");
    // assertThrows(AssertionFailedError.class, () -> connection.assertCountsPrefixEquals(1, "1"));
    // }
    //
    // @Test
    // void assertCountsExactWhenEquals(@ContainerRedisConnection RedisConnection connection) {
    // connection.get().sadd("11", "1");
    // assertDoesNotThrow(() -> connection.assertCountsPrefixEquals(1, "1"));
    // }
    //
    // @Test
    // void assertCountsExactWhenMore(@ContainerRedisConnection RedisConnection connection) {
    // connection.get().sadd("11", "1");
    // connection.get().sadd("12", "2");
    // assertThrows(AssertionFailedError.class, () -> connection.assertCountsPrefixEquals(1, "1"));
    // }
    //
    // @Test
    // void assertCountsExactWhenEquals(@ContainerRedisConnection RedisConnection connection) {
    // connection.get().sadd("11", "1");
    // assertDoesNotThrow(() -> connection.assertCountsPrefixEquals(1, "1"));
    // }
}
