package tsg.rest.aggregator;

import tsg.rest.controller.REST_ENDPOINT;
import tsg.rest.controller.RestController;
import tsg.rest.json.JsonHelper;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AggregatedDataService {

    private final RestController restController;
    private final RedisCache redisCache;

    private static AggregatedDataService INSTANCE;

    public static AggregatedDataService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AggregatedDataService();
        }

        return INSTANCE;
    }

    private AggregatedDataService() {
        this.restController = RestController.getInstance();
        this.redisCache = null;
    }

    public String getAggregatedResponse() {
        CompletableFuture<String> weatherFuture = buildComparableFuture(REST_ENDPOINT.WEATHER_URL);
        CompletableFuture<String> randomFuture = buildComparableFuture(REST_ENDPOINT.RANDOM_URL);
        CompletableFuture<String> ipFuture = buildComparableFuture(REST_ENDPOINT.IP_URL);

        return Stream.of(weatherFuture, randomFuture, ipFuture)
                .map(CompletableFuture::join)
                .collect(Collectors.joining(" "));
    }

    private CompletableFuture<String> buildComparableFuture(REST_ENDPOINT endpoint) {

        return restController.fetchAsync(endpoint.getValue())
                .exceptionally(exception -> {
                    exception.printStackTrace();
                    String messageFromCache = "";
                    if (redisCache != null) {
                        messageFromCache = redisCache.getCachedData(endpoint.getKey()).join();
                    }

                    return messageFromCache;
                }).thenApply(result -> {
                    Optional.ofNullable(redisCache).ifPresent(cache -> cache.cacheData(endpoint.getKey(), result));
                    return JsonHelper.addNodeToKey(endpoint.getKey(), result);
                });
    }
}
