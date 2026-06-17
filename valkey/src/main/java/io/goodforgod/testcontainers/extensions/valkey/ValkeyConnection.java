package io.goodforgod.testcontainers.extensions.valkey;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.util.JedisURIHelper;

/**
 * Describes active Valkey connection of currently running {@link ValkeyContainer}
 */
public interface ValkeyConnection extends AutoCloseable {

    /**
     * Valkey connection parameters
     */
    interface Params {

        @NotNull
        URI uri();

        @NotNull
        String host();

        int port();

        String username();

        String password();

        int database();
    }

    @NotNull
    Params params();

    @NotNull
    Optional<Params> paramsInNetwork();

    @NotNull
    JedisConnection getConnection();

    @NotNull
    default JedisConnection commands() {
        return getConnection();
    }

    void deleteAll();

    int countPrefix(@NotNull ValkeyKey keyPrefix);

    int count(@NotNull ValkeyKey... keys);

    int count(@NotNull Collection<ValkeyKey> keys);

    void assertCountsPrefixNone(@NotNull ValkeyKey keyPrefix);

    void assertCountsNone(@NotNull ValkeyKey... keys);

    void assertCountsNone(@NotNull Collection<ValkeyKey> keys);

    List<ValkeyValue> assertCountsPrefixAtLeast(long expectedAtLeast, @NotNull ValkeyKey keyPrefix);

    List<ValkeyValue> assertCountsPrefixEquals(long expected, @NotNull ValkeyKey keyPrefix);

    List<ValkeyValue> assertCountsAtLeast(long expectedAtLeast, @NotNull ValkeyKey... keys);

    List<ValkeyValue> assertCountsAtLeast(long expectedAtLeast, @NotNull Collection<ValkeyKey> keys);

    List<ValkeyValue> assertCountsEquals(long expected, @NotNull ValkeyKey... keys);

    List<ValkeyValue> assertCountsEquals(long expected, @NotNull Collection<ValkeyKey> keys);

    @Override
    void close();

    static ValkeyConnection forContainer(ValkeyContainer container) {
        if (!container.isRunning()) {
            throw new IllegalStateException(container.getClass().getSimpleName() + " container is not running");
        }

        var params = new ValkeyConnectionImpl.ParamsImpl(container.getHost(), container.getPort(), container.getUser(),
                container.getPassword(), container.getDatabase());
        final String alias = container.getNetworkAliases().get(container.getNetworkAliases().size() - 1);
        var network = new ValkeyConnectionImpl.ParamsImpl(alias, ValkeyContainer.PORT, container.getUser(),
                container.getPassword(),
                container.getDatabase());
        return new ValkeyConnectionClosableImpl(params, network);
    }

    static ValkeyConnection forURI(URI uri) {
        HostAndPort hostAndPort = JedisURIHelper.getHostAndPort(uri);
        String user = JedisURIHelper.getUser(uri);
        String password = JedisURIHelper.getPassword(uri);
        int database = JedisURIHelper.getDBIndex(uri);
        var params = new ValkeyConnectionImpl.ParamsImpl(hostAndPort.getHost(), hostAndPort.getPort(), user, password, database);
        return new ValkeyConnectionClosableImpl(params, null);
    }

    static ValkeyConnection forParams(String host,
                                      int port,
                                      int database,
                                      String username,
                                      String password) {
        var params = new ValkeyConnectionImpl.ParamsImpl(host, port, username, password, database);
        return new ValkeyConnectionClosableImpl(params, null);
    }
}
