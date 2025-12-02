package tsg.rest.aggregator;

import java.util.concurrent.CompletableFuture;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;

public class RedisCache {

    public final RedisConfig config = new RedisConfig();
    public final StatefulRedisConnection<String, String> connection = config.getConnection();
    private final long expirationSeconds = 300;

    public CompletableFuture<Void> cacheData(String key, String value) {
        RedisAsyncCommands<String, String> commands = connection.async();
        return commands.set(key, value)
                .thenCompose(ok -> commands.expire(key, expirationSeconds))
                .thenAccept(expired -> {})
                .toCompletableFuture();

    }

    public CompletableFuture<String> getCachedData(String key) {
        RedisAsyncCommands<String, String> commands = connection.async();
        return commands.get(key).toCompletableFuture();

    }
}