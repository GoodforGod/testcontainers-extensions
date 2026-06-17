package io.goodforgod.testcontainers.extensions.valkey;

import static org.junit.jupiter.api.Assertions.*;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

@TestcontainersValkey(mode = ContainerMode.PER_CLASS, image = "valkey/valkey:8.1-alpine")
class ValkeyConnectionAssertsTests {

    @ConnectionValkey
    private ValkeyConnection connection;

    @BeforeEach
    void clean() {
        connection.deleteAll();
    }

    @Test
    void countPrefix() {
        connection.commands().set("11", "1");
        connection.commands().set("12", "2");
        assertEquals(2, connection.countPrefix(ValkeyKey.of("1")));
    }

    @Test
    void assertCountsPrefixNoneWhenMore() {
        connection.commands().set("11", "1");
        connection.commands().set("12", "2");
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsPrefixNone(ValkeyKey.of("1")));
    }

    @Test
    void assertCountsPrefixNoneWhenZero() {
        assertDoesNotThrow(() -> connection.assertCountsPrefixNone(ValkeyKey.of("1")));
    }

    @Test
    void assertCountsPrefixAtLeastWhenZero() {
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsPrefixAtLeast(1, ValkeyKey.of("1")));
    }

    @Test
    void assertCountsPrefixAtLeastWhenMore() {
        connection.commands().set("11", "1");
        connection.commands().set("12", "2");
        assertDoesNotThrow(() -> connection.assertCountsPrefixAtLeast(1, ValkeyKey.of("1")));
    }

    @Test
    void assertCountsPrefixAtLeastWhenEquals() {
        connection.commands().set("11", "1");
        assertDoesNotThrow(() -> connection.assertCountsPrefixAtLeast(1, ValkeyKey.of("1")));
    }

    @Test
    void assertCountsPrefixExactWhenZero() {
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsPrefixEquals(1, ValkeyKey.of("1")));
    }

    @Test
    void count() {
        connection.commands().set("11", "1");
        connection.commands().set("12", "2");
        assertEquals(1, connection.count(ValkeyKey.of("11")));
    }

    @Test
    void assertCountsNoneWhenMore() {
        connection.commands().set("11", "1");
        connection.commands().set("12", "2");
        var k1 = ValkeyKey.of("11");
        var k2 = ValkeyKey.of("12");
        assertNotEquals(k1, k2);
        assertNotEquals(k1.toString(), k2.toString());
        assertNotEquals(k1.hashCode(), k2.hashCode());
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsNone(List.of(k1, k2)));
    }

    @Test
    void assertCountsNoneWhenZero() {
        assertDoesNotThrow(() -> connection.assertCountsNone(ValkeyKey.of("1")));
    }

    @Test
    void assertCountsAtLeastWhenZero() {
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsAtLeast(1, ValkeyKey.of("1")));
    }

    @Test
    void assertCountsAtLeastWhenOther() {
        connection.commands().set("11", "{\"a\":1}");
        connection.commands().set("12", "{\"a\":2}");
        assertDoesNotThrow(() -> connection.assertCountsAtLeast(1, ValkeyKey.of("11", "22")));
    }

    @Test
    void assertCountsAtLeastWhenMore() {
        connection.commands().set("11", "{\"a\":1}");
        connection.commands().set("12", "{\"a\":2}");
        var values = assertDoesNotThrow(() -> connection.assertCountsAtLeast(1, ValkeyKey.of("11", "12")));
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
        assertDoesNotThrow(() -> connection.assertCountsAtLeast(1, ValkeyKey.of("11")));
    }

    @Test
    void assertCountsExactWhenZero() {
        assertThrows(AssertionFailedError.class, () -> connection.assertCountsEquals(1, ValkeyKey.of("1")));
    }
}
