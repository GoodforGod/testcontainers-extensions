package io.goodforgod.testcontainers.extensions.redis;

import redis.clients.jedis.commands.*;

/**
 * @see redis.clients.jedis.Jedis
 */
public interface JedisConnection extends
        ServerCommands,
        DatabaseCommands,
        JedisCommands,
        JedisBinaryCommands,
        ControlCommands,
        ControlBinaryCommands,
        ClusterCommands,
        ModuleCommands,
        GenericControlCommands,
        SentinelCommands {

}
