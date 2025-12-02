package tsg.rest.aggregator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class ApiAggregator {
    private static final String WEATHER_URL = "https://api.open-meteo.com/v1/forecast?latitude=51.107883&longitude=17.038538&current_weather=true";
    private static final String RANDOM_FACT_URL = "https://uselessfacts.jsph.pl/api/v2/facts/random";
    private static final String IP_URL = "https://api.ipify.org/?format=json";

    private final Duration timeoutLimit = Duration.ofSeconds(5);
    private final HttpClient httpClient;
    private final RedisCache redisCache;
    private final ObjectMapper mapper = new ObjectMapper();

    public ApiAggregator() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeoutLimit)
                .build();
        this.redisCache = new RedisCache();
    }

    public CompletableFuture<String> aggregateData() {
        Map<String, CompletableFuture<JsonNode>> futures = new HashMap<>();
        futures.put("weather", fetchWithRetryAndCache(WEATHER_URL, "weather"));
        futures.put("fact", fetchWithRetryAndCache(RANDOM_FACT_URL, "randomFact"));
        futures.put("ip", fetchWithRetryAndCache(IP_URL, "ip"));

        CompletableFuture<?>[] futuresArray = futures.values().toArray(new CompletableFuture<?>[0]);

        return CompletableFuture.allOf(futuresArray)
                .thenApply(v -> {
                    ObjectNode root = mapper.createObjectNode();
                    futures.forEach((key, future) -> {
                        try {
                            root.set(key, future.join()); // Safe after allOf
                        } catch (Exception e) {
                            root.putObject(key).put("error", "Failed to fetch: " + e.getMessage());
                        }
                    });
                    return JsonUtils.toJson(root);
                })
                .exceptionally(e -> JsonUtils.toJson(mapper.createObjectNode().put("error", e.getMessage())));
    }

    private CompletableFuture<JsonNode> fetchWithRetryAndCache(String url, String cacheKey) {
        return fetchAsync(url)
                .exceptionally(ex -> null) // On error, null to trigger cache
                .thenCompose(response -> {
                    if (response != null) {
                        try {
                            JsonNode json = mapper.readTree(response);
                            redisCache.cacheData(cacheKey, json.toString());
                            return CompletableFuture.completedFuture(json);
                        } catch (Exception e) {
                            return fallbackToCache(cacheKey, e);
                        }
                    } else {
                        return fallbackToCache(cacheKey, null);
                    }
                })
                .exceptionally(e -> mapper.createObjectNode().put("error", e.getMessage())); // Final fallback
    }

    private CompletableFuture<JsonNode> fallbackToCache(String cacheKey, Throwable originalError) {
        return redisCache.getCachedData(cacheKey)
                .thenApply(cached -> {
                    if (cached != null) {
                        try {
                            return mapper.readTree(cached);
                        } catch (Exception e) {
                            return mapper.createObjectNode().put("error", "Cache invalid: " + e.getMessage());
                        }
                    } else {
                        return mapper.createObjectNode().put("error",
                                originalError != null ? originalError.getMessage() : "No cache available");
                    }
                });
    }

    private CompletableFuture<String> fetchAsync(String url) {
        return fetchSingle(url)
                .exceptionally(ex -> null) // Pierwsza próba
                .thenCompose(res -> {
                    if (res == null) {
                        return fetchSingle(url); // Retry (druga próba)
                    }
                    return CompletableFuture.completedFuture(res);
                });
    }

    private CompletableFuture<String> fetchSingle(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeoutLimit)
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }
}