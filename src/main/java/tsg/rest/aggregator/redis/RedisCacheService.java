package tsg.rest.aggregator.redis;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;

public class RedisCacheService {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);
    public final RedisConfig config = new RedisConfig();
    public final StatefulRedisConnection<String, String> connection = config.getConnection();
    private final long expirationSeconds = 300;

    public void cacheData(String key, String value) {
        RedisAsyncCommands<String, String> commands = connection.async();
        commands.set(key, value)
                .thenCompose(ok -> commands.expire(key, expirationSeconds))
                .exceptionally(throwable -> {
                    log.error("Error putting in cache, value: {}", value);
                    return null;
                });

    }

    public CompletableFuture<String> getCachedData(String key) {
        RedisAsyncCommands<String, String> commands = connection.async();
        return commands
                .get(key)
                .exceptionally(throwable ->
                {
                    log.error("Error getting cached value");
                    return "{\"error\"}";
                })
                .toCompletableFuture();
    }

}