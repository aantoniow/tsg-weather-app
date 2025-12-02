package tsg.rest.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class RestController {

    private final HttpClient httpClient;
    private final Duration timeoutLimit;

    private static RestController INSTANCE;

    private RestController() {
        this.timeoutLimit = Duration.ofSeconds(5);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.timeoutLimit)
                .build();
    }

    public static RestController getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RestController();
        }

        return INSTANCE;
    }

    public CompletableFuture<String> fetchAsync(String url) {
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

}
