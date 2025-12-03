package tsg.rest.aggregator;

import java.util.concurrent.CompletableFuture;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;

public final class RedisCache {

    public final RedisConfig config;
    public final StatefulRedisConnection<String, String> connection;
    private final long expirationSeconds = 300;

    private static RedisCache INSTANCE;

    private RedisCache() {
        config = new RedisConfig();
        connection = config.getConnection();
    }

    public static RedisCache getInstance() {
        if (INSTANCE == null) {
            try {
                INSTANCE = new RedisCache();
            } catch (Exception exception) {
                exception.printStackTrace();
                System.err.println("Redis failed to initialize");
            }
        }

        return INSTANCE;
    }

    public CompletableFuture<Void> cacheData(String key, String value) {
        RedisAsyncCommands<String, String> commands = connection.async();
        return commands.set(key, value)
                .thenCompose(ok -> commands.expire(key, expirationSeconds))
                .thenAccept(expired -> {
                })
                .toCompletableFuture();
    }

    public CompletableFuture<String> getCachedData(String key) {
        RedisAsyncCommands<String, String> commands = connection.async();
        return commands.get(key).toCompletableFuture();
    }
}