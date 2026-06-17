package io.goodforgod.testcontainers.extensions.valkey;

import org.jetbrains.annotations.ApiStatus.Internal;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;

@Internal
final class JedisCommandsImpl extends Jedis implements JedisConnection {

    JedisCommandsImpl(final HostAndPort hostPort, final JedisClientConfig config) {
        super(hostPort, config);
    }
}
