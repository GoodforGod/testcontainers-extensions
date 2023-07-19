package io.goodforgod.testcontainers.extensions.kafka;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

interface KafkaConnection {

    interface Consumer {

        void reset();

        @NotNull
        List<ReceivedEvent> receivedPreviously();

        @NotNull
        default Optional<ReceivedEvent> getReceived() {
            return getReceived(Duration.ofSeconds(15));
        }

        @NotNull
        Optional<ReceivedEvent> getReceived(@NotNull Duration timeout);

        @NotNull
        default List<ReceivedEvent> getReceivedAtLeast(int expectedAtLeast) {
            return getReceivedAtLeast(expectedAtLeast, Duration.ofSeconds(15));
        }

        @NotNull
        List<ReceivedEvent> getReceivedAtLeast(int expectedAtLeast, @NotNull Duration timeout);

        void assertReceivedNone(@NotNull Duration timeToWait);

        @NotNull
        default ReceivedEvent assertReceived() {
            return assertReceived(Duration.ofSeconds(15));
        }

        @NotNull
        ReceivedEvent assertReceived(@NotNull Duration timeout);

        @NotNull
        default List<ReceivedEvent> assertReceivedAtLeast(int expectedAtLeast) {
            return assertReceivedAtLeast(expectedAtLeast, Duration.ofSeconds(15));
        }

        @NotNull
        List<ReceivedEvent> assertReceivedAtLeast(int expectedAtLeast, @NotNull Duration timeout);

        @NotNull
        List<ReceivedEvent> assertReceivedEqualsInTime(int expected, @NotNull Duration timeToWait);

        boolean checkReceivedNone(@NotNull Duration timeToWait);

        default boolean checkReceived() {
            return checkReceived(Duration.ofSeconds(15));
        }

        boolean checkReceived(@NotNull Duration timeout);

        default boolean checkReceivedAtLeast(int expectedAtLeast) {
            return checkReceivedAtLeast(expectedAtLeast, Duration.ofSeconds(15));
        }

        boolean checkReceivedAtLeast(int expectedAtLeast, @NotNull Duration timeout);

        boolean checkReceivedEqualsInTime(int expected, @NotNull Duration timeToWait);
    }

    void send(@NotNull String topic, @NotNull Event... events);

    void send(@NotNull String topic, @NotNull List<Event> events);

    @NotNull
    Consumer subscribe(@NotNull String... topics);

    @NotNull
    Consumer subscribe(@NotNull List<String> topics);
}
