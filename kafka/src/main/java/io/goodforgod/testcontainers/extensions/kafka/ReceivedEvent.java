package io.goodforgod.testcontainers.extensions.kafka;

import java.time.OffsetDateTime;
import org.jetbrains.annotations.NotNull;

public interface ReceivedEvent extends Event {

    @NotNull
    String topic();

    int partition();

    long offset();

    long timestamp();

    /**
     * @return timestamp as {@link OffsetDateTime}
     */
    @NotNull
    OffsetDateTime datetime();
}
