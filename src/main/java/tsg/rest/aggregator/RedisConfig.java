package tsg.rest.aggregator;

import java.time.Duration;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

public class RedisConfig {
    private final RedisClient redisClient;
    public final StatefulRedisConnection<String, String> connection;

    public RedisConfig() {
        String host = System.getenv().getOrDefault("REDIS_HOST", "redis");
        int port = Integer
                .parseInt(System.getenv().getOrDefault("REDIS_PORT", String.valueOf(RedisURI.DEFAULT_REDIS_PORT)));

        RedisURI uri = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withTimeout(timeoutLimit)
                .build();

        this.redisClient = RedisClient.create(uri);
        this.connection = redisClient.connect();
    }

    public StatefulRedisConnection<String, String> getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null)
                connection.close();
        } finally {
            if (redisClient != null)
                redisClient.shutdown();
        }
    }

    private final Duration timeoutLimit = Duration.ofSeconds(5);

}