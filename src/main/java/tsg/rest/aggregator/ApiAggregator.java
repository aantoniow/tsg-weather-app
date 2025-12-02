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
        Map<String, CompletableFuture<String>> futures = new HashMap<>();
        futures.put("weather", fetchWithRetryAndCache(WEATHER_URL, "weather"));
        futures.put("fact", fetchWithRetryAndCache(RANDOM_FACT_URL, "randomFact"));
        futures.put("ip", fetchWithRetryAndCache(IP_URL, "ip"));

        CompletableFuture<?>[] futuresArray = futures.values().toArray(new CompletableFuture<?>[0]);

        return CompletableFuture.allOf(futuresArray)
                .thenApply(v -> {
                    ObjectNode root = mapper.createObjectNode();
                    futures.forEach((key, future) -> {
                        try {
                            String jsonStr = future.join(); // bezpieczne po allOf
                            if (jsonStr == null) {
                                root.putObject(key).put("error", "null response");
                            } else {
                                try {
                                    JsonNode node = mapper.readTree(jsonStr);
                                    root.set(key, node);
                                } catch (Exception parseEx) {
                                    root.putObject(key).put("error", "Invalid JSON: " + parseEx.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            root.putObject(key).put("error", "Failed to fetch: " + e.getMessage());
                        }
                    });
                    try {
                        return mapper.writeValueAsString(root);
                    } catch (Exception e) {
                        // bardzo defensywnie
                        return "{\"error\":\"Failed to serialize aggregated JSON: " + e.getMessage() + "\"}";
                    }
                })
                .exceptionally(e -> {
                    // jeżeli allOf rzucił jakiś nieoczekiwany błąd
                    ObjectNode err = mapper.createObjectNode().put("error", e.getMessage());
                    try {
                        return mapper.writeValueAsString(err);
                    } catch (Exception ex) {
                        return "{\"error\":\"Unexpected failure\"}";
                    }
                });
    }

    private CompletableFuture<String> fetchWithRetryAndCache(String url, String cacheKey) {
        return fetchAsync(url)
                .thenCompose(response -> {
                    if (response != null) {
                        // sukces - próbujemy sparsować i zapisać do cache
                        try {
                            // walidacja JSON: parsujemy by mieć pewność
                            mapper.readTree(response);
                            // zapis do cache, ale nie blokujemy: zwróćmy response dopiero po zapisaniu
                            return redisCache.cacheData(cacheKey, response)
                                    .handle((v, err) -> response);
                        } catch (Exception e) {
                            // parsowanie fail -> fallback do cache
                            return fallbackToCache(cacheKey, e);
                        }
                    } else {
                        return fallbackToCache(cacheKey, null);
                    }
                })
                .exceptionally(e -> {
                    // ostatnia deska ratunku: zwróć JSON z błędem
                    ObjectNode err = mapper.createObjectNode().put("error", e.getMessage());
                    return err.toString();
                });
    }

    private CompletableFuture<String> fallbackToCache(String cacheKey, Throwable originalError) {
        return redisCache.getCachedData(cacheKey)
                .thenApply(cached -> {
                    if (cached != null) {
                        return cached;
                    } else {
                        ObjectNode err = mapper.createObjectNode();
                        err.put("error", originalError != null ? originalError.getMessage() : "No cache available");
                        return err.toString();
                    }
                });
    }

    private CompletableFuture<String> fetchAsync(String url) {
        return fetchSingle(url)
                .exceptionally(ex -> null) // pierwsza próba - jeśli nie, zwróć null
                .thenCompose(res -> {
                    if (res == null) {
                        // retry
                        return fetchSingle(url).exceptionally(ex -> null);
                    }
                    return CompletableFuture.completedFuture(res);
                });
    }

    private CompletableFuture<String> fetchSingle(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeoutLimit)
                .GET()
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .exceptionally(ex -> null);
    }

    public void close() {
        try {
            redisCache.close();
        } catch (Exception ignored) {}
        // HttpClient nie wymaga zamykania
    }

}