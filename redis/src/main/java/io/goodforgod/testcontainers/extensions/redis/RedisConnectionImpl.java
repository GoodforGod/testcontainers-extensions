package io.goodforgod.testcontainers.extensions.redis;

import java.time.Duration;
import java.util.*;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.args.FlushMode;

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
            return "[host=" + host +
                    ", port=" + port +
                    ", username=" + username +
                    ", password=" + password + ']';
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(RedisConnection.class);

    private final Params params;
    private final Params network;
    private final RedisCommandsImpl jedis;

    RedisConnectionImpl(Params params, Params network) {
        this.params = params;
        this.network = network;

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

        this.jedis = new RedisCommandsImpl(new HostAndPort(params().host(), params().port()), config.build());
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
    public RedisCommands commands() {
        return jedis;
    }

    @Override
    public void deleteAll() {
        jedis.flushAll(FlushMode.SYNC);
    }

    private List<String> getValuesByPrefix(@NotNull String keyPrefix) {
        final Set<String> keys = jedis.keys(keyPrefix + "*");
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }

        return jedis.mget(keys.toArray(String[]::new));
    }

    @Override
    public int countPrefix(@NotNull String keyPrefix) {
        return getValuesByPrefix(keyPrefix).size();
    }

    @Override
    public int count(@NotNull String... keys) {
        return jedis.mget(keys).size();
    }

    @Override
    public int count(@NotNull Collection<String> keys) {
        return count(keys.toArray(String[]::new));
    }

    @Override
    public void assertCountsPrefixNone(@NotNull String keyPrefix) {
        assertCountsPrefixEquals(0, keyPrefix);
    }

    @Override
    public void assertCountsNone(@NotNull String... keys) {
        final List<String> keyToValue = jedis.mget(keys);
        final long count = keyToValue.size();
        Assertions.assertEquals(0, count, String.format("Expected to count 0 for keys %s but found %s",
                Arrays.toString(keys), count));
    }

    @Override
    public void assertCountsNone(@NotNull Collection<String> keys) {
        assertCountsNone(keys.toArray(String[]::new));
    }

    @Override
    public List<String> assertCountsPrefixAtLeast(long expectedAtLeast, @NotNull String keyPrefix) {
        final List<String> keyToValue = getValuesByPrefix(keyPrefix);
        final long count = keyToValue.size();
        if (count < expectedAtLeast) {
            Assertions.assertEquals(expectedAtLeast, count,
                    String.format("Expected to count for prefix '%s' at least %s values but received %s",
                            keyPrefix, expectedAtLeast, count));
        }

        return keyToValue;
    }

    @Override
    public List<String> assertCountsPrefixEquals(long expected, @NotNull String keyPrefix) {
        final List<String> keyToValue = getValuesByPrefix(keyPrefix);
        final long count = keyToValue.size();
        Assertions.assertEquals(expected, count, String.format("Expected to count for '%s' prefix %s values but received %s",
                keyPrefix, expected, count));
        return keyToValue;
    }

    @Override
    public List<String> assertCountsAtLeast(long expectedAtLeast, @NotNull String... keys) {
        final List<String> values = jedis.mget(keys);
        final long count = values.size();
        if (count < expectedAtLeast) {
            Assertions.assertEquals(expectedAtLeast, count,
                    String.format("Expected to count at least %s values but received %s for keys %s",
                            expectedAtLeast, count, Arrays.toString(keys)));
        }

        return values;
    }

    @Override
    public List<String> assertCountsEquals(long expected, @NotNull String... keys) {
        final List<String> values = jedis.mget(keys);
        final long count = values.size();
        Assertions.assertEquals(expected, count, String.format("Expected to count %s values but received %s for keys %s",
                expected, count, Arrays.toString(keys)));
        return values;
    }

    @Override
    public List<String> assertCountsAtLeast(long expectedAtLeast, @NotNull Collection<String> keys) {
        return assertCountsAtLeast(expectedAtLeast, keys.toArray(String[]::new));
    }

    @Override
    public List<String> assertCountsEquals(long expected, @NotNull Collection<String> keys) {
        return assertCountsEquals(expected, keys.toArray(String[]::new));
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
        jedis.close();
    }
}
