package io.goodforgod.testcontainers.extensions.redis;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.args.FlushMode;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Internal
final class RedisConnectionImpl implements RedisConnection {

    private static final class ParamsImpl implements Params {

        private final String host;
        private final int port;
        private final String username;
        private final String password;
        private final int database;

        ParamsImpl(String host, int port, String username, String password, int database) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.database = database;
        }

        @Override
        public @NotNull URI uri() {
            return (username() == null && password() == null)
                    ? URI.create(String.format("redis://%s:%s/%s", host(), port(), database()))
                    : URI.create(String.format("redis://%s:%s@%s:%s/%s", username(), password(), host(), port(), database()));
        }

        @Override
        public @NotNull String host() {
            return host;
        }

        @Override
        public int port() {
            return port;
        }

        @Override
        public String username() {
            return username;
        }

        @Override
        public String password() {
            return password;
        }

        @Override
        public int database() {
            return database;
        }

        @Override
        public String toString() {
            return uri().toString();
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(RedisConnection.class);

    private final Params params;
    private final Params network;

    private volatile RedisCommandsImpl jedis;

    RedisConnectionImpl(Params params, Params network) {
        this.params = params;
        this.network = network;
    }

    static RedisConnection forContainer(String host,
                                        int port,
                                        String hostInNetwork,
                                        Integer portInNetwork,
                                        int database,
                                        String username,
                                        String password) {
        var params = new ParamsImpl(host, port, username, password, database);
        final Params network;
        if (hostInNetwork == null) {
            network = null;
        } else {
            network = new ParamsImpl(hostInNetwork, portInNetwork, username, password, database);
        }

        return new RedisConnectionImpl(params, network);
    }

    static RedisConnection forExternal(String host,
                                       int port,
                                       int database,
                                       String username,
                                       String password) {
        var params = new ParamsImpl(host, port, username, password, database);
        return new RedisConnectionImpl(params, null);
    }

    @Override
    public @NotNull Params params() {
        return params;
    }

    @Override
    public @NotNull Optional<Params> paramsInNetwork() {
        return Optional.ofNullable(network);
    }

    @NotNull
    private RedisCommands connection() {
        if (jedis == null) {
            try {
                var config = DefaultJedisClientConfig.builder()
                        .timeoutMillis((int) Duration.ofSeconds(10).toMillis())
                        .blockingSocketTimeoutMillis((int) Duration.ofSeconds(10).toMillis())
                        .clientName("testcontainers-extensions-redis")
                        .database(Protocol.DEFAULT_DATABASE);

                if (params().username() != null) {
                    config.user(params.username());
                }

                if (params().password() != null) {
                    config.password(params().password());
                }

                jedis = new RedisCommandsImpl(new HostAndPort(params().host(), params().port()), config.build());
            } catch (Exception e) {
                throw new RedisConnectionException(e);
            }
        }

        return jedis;
    }

    @NotNull
    public RedisCommands commands() {
        return connection();
    }

    @Override
    public void deleteAll() {
        try {
            connection().flushAll(FlushMode.SYNC);
        } catch (RedisConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisConnectionException(e);
        }
    }

    private List<RedisValue> getValuesByKeys(@NotNull Collection<RedisKey> keys) {
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }

        final byte[][] keysAsBytes = keys.stream()
                .map(RedisKey::asBytes)
                .toArray(byte[][]::new);

        try {
            logger.debug("Looking for keys: {}", keys);
            return connection().mget(keysAsBytes).stream()
                    .filter(Objects::nonNull)
                    .map(RedisValueImpl::new)
                    .collect(Collectors.toList());
        } catch (RedisConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisConnectionException(e);
        }
    }

    private List<RedisValue> getValuesByPrefix(RedisKey keyPrefix) {
        try {
            final byte[] prefix = (keyPrefix.asString() + "*").getBytes(StandardCharsets.UTF_8);
            final List<RedisKey> keys = connection().keys(prefix).stream()
                    .filter(Objects::nonNull)
                    .map(RedisKey::of)
                    .collect(Collectors.toList());

            return getValuesByKeys(keys);
        } catch (RedisConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisConnectionException(e);
        }
    }

    @Override
    public int countPrefix(@NotNull RedisKey keyPrefix) {
        return getValuesByPrefix(keyPrefix).size();
    }

    @Override
    public int count(@NotNull RedisKey... keys) {
        return count(List.of(keys));
    }

    @Override
    public int count(@NotNull Collection<RedisKey> keys) {
        return Math.toIntExact(getValuesByKeys(keys).stream()
                .filter(Objects::nonNull)
                .count());
    }

    @Override
    public void assertCountsPrefixNone(@NotNull RedisKey keyPrefix) {
        assertCountsPrefixEquals(0, keyPrefix);
    }

    @Override
    public void assertCountsNone(@NotNull RedisKey... keys) {
        assertCountsNone(Arrays.asList(keys));
    }

    @Override
    public void assertCountsNone(@NotNull Collection<RedisKey> keys) {
        final List<RedisValue> values = getValuesByKeys(keys);
        final long count = values.size();
        Assertions.assertEquals(0, count, String.format("Expected to count 0 for keys %s but found %s",
                keys, count));
    }

    @Override
    public List<RedisValue> assertCountsPrefixAtLeast(long expectedAtLeast, @NotNull RedisKey keyPrefix) {
        final List<RedisValue> keyToValue = getValuesByPrefix(keyPrefix);
        final long count = keyToValue.size();
        if (count < expectedAtLeast) {
            Assertions.assertEquals(expectedAtLeast, count,
                    String.format("Expected to count for prefix '%s' at least %s values but received %s",
                            keyPrefix, expectedAtLeast, count));
        }

        return keyToValue;
    }

    @Override
    public List<RedisValue> assertCountsPrefixEquals(long expected, @NotNull RedisKey keyPrefix) {
        final List<RedisValue> keyToValue = getValuesByPrefix(keyPrefix);
        final long count = keyToValue.size();
        Assertions.assertEquals(expected, count, String.format("Expected to count for '%s' prefix %s values but received %s",
                keyPrefix, expected, count));
        return keyToValue;
    }

    @Override
    public List<RedisValue> assertCountsAtLeast(long expectedAtLeast, @NotNull RedisKey... keys) {
        return assertCountsAtLeast(expectedAtLeast, List.of(keys));
    }

    @Override
    public List<RedisValue> assertCountsAtLeast(long expectedAtLeast, @NotNull Collection<RedisKey> keys) {
        final List<RedisValue> values = getValuesByKeys(keys);
        final long count = values.size();
        if (count < expectedAtLeast) {
            Assertions.assertEquals(expectedAtLeast, count,
                    String.format("Expected to count at least %s values but received %s for keys %s",
                            expectedAtLeast, count, keys));
        }

        return values;
    }

    @Override
    public List<RedisValue> assertCountsEquals(long expected, @NotNull RedisKey... keys) {
        return assertCountsEquals(expected, List.of(keys));
    }

    @Override
    public List<RedisValue> assertCountsEquals(long expected, @NotNull Collection<RedisKey> keys) {
        final List<RedisValue> values = getValuesByKeys(keys);
        final long count = values.size();
        Assertions.assertEquals(expected, count, String.format("Expected to count %s values but received %s for keys %s",
                expected, count, keys));
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RedisConnectionImpl that = (RedisConnectionImpl) o;
        return Objects.equals(params, that.params) && Objects.equals(network, that.network);
    }

    @Override
    public int hashCode() {
        return Objects.hash(params, network);
    }

    @Override
    public String toString() {
        return params().toString();
    }

    void close() {
        if (jedis != null) {
            jedis.close();
        }
    }
}
