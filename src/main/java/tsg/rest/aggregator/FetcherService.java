package tsg.rest.aggregator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetcherService {
    private static final Logger log = LoggerFactory.getLogger(FetcherService.class);
    private final String KEY;
    private final String URL;
    private final Duration TIMEOUT = Duration.ofSeconds(3);

    private final RedisCache redisCache;
    private final HttpClient httpClient;

    public FetcherService(String key, String url) {
        this.httpClient = HttpClient.newHttpClient();
        this.redisCache = new RedisCache();
        this.KEY = key;
        this.URL = url;
    }

    public CompletableFuture<String> fetchSingle() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .timeout(TIMEOUT)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> JsonUtils.toJson(KEY, body))
                .thenCompose(body -> {
                    CompletableFuture.runAsync(() -> redisCache.cacheData(KEY, body));
                    return CompletableFuture.completedFuture(body);
                })
                .exceptionally(e -> {
                    log.warn("Error reading cache, value");
                    return String.valueOf(CompletableFuture.supplyAsync(() -> redisCache.getCachedData(KEY)));
                });
    }

}