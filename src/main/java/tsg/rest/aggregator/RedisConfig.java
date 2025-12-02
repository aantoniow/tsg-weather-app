package tsg.rest.aggregator;

import java.time.Duration;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

public class RedisConfig {
    private final RedisClient redisClient;
    public final StatefulRedisConnection<String, String> connection;

    public StatefulRedisConnection<String, String> getConnection() {
        return connection;
    }

    private final Duration timeoutLimit = Duration.ofSeconds(5);

    public RedisConfig() {
        RedisURI uri = RedisURI.builder()
                .withHost("localhost")
                .withPort(RedisURI.DEFAULT_REDIS_PORT)
                .withTimeout(timeoutLimit)
                .build();

        this.redisClient = RedisClient.create(uri);
        this.connection = redisClient.connect();
    }
}