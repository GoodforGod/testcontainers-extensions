package io.goodforgod.testcontainers.extensions.redis;

import org.jetbrains.annotations.ApiStatus.Internal;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;

@Internal
final class RedisCommandsImpl extends Jedis implements RedisCommands {

    RedisCommandsImpl(final HostAndPort hostPort, final JedisClientConfig config) {
        super(hostPort, config);
    }
}
