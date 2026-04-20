package tsg.rest.aggregator.restclient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tsg.rest.aggregator.JsonUtils;
import tsg.rest.aggregator.redis.RedisCacheService;

public class FetcherService {
    private static final Logger log = LoggerFactory.getLogger(FetcherService.class);
    private final Duration TIMEOUT = Duration.ofSeconds(3);

    private final RedisCacheService redisCache;
    private final HttpClient httpClient;

    public FetcherService() {
        this.httpClient = HttpClient.newHttpClient();
        this.redisCache = new RedisCacheService();
    }

    public CompletableFuture<String> fetchSingle(Endpoints endpoint) {
        String url = endpoint.url();
        String key = endpoint.key();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> JsonUtils.toJson(key, body))
                .thenCompose(json -> {
                    CompletableFuture.runAsync(() -> redisCache.cacheData(key, json));
                    return CompletableFuture.completedFuture(json);
                })
                .exceptionally(throwable -> {
                    log.warn("Upstream failed for {}, falling back to cache", key, throwable);
                    String cached = redisCache.getCachedData(key).join();

                    if (cached != null && !cached.isBlank()) {
                        log.debug("Returning cached data for {}", key);
                        return cached;
                    } else {
                        log.warn("No cached data for {}", key);
                        return JsonUtils.errorJson(key, throwable.getMessage());
                    }
                });
    }

}