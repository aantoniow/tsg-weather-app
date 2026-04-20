package tsg.rest.aggregator;

import java.util.concurrent.CompletableFuture;

import tsg.rest.aggregator.dto.AggregateResponse;
import tsg.rest.aggregator.restclient.Endpoints;
import tsg.rest.aggregator.restclient.FetcherService;

public class AggregateService {

    private final FetcherService fetcher;

    public AggregateService(FetcherService fetcher) {
        this.fetcher = fetcher;
    }

    public CompletableFuture<AggregateResponse> getAggregatedData() {
        CompletableFuture<String> weather = fetcher.fetchSingle(Endpoints.WEATHER);
        CompletableFuture<String> fact = fetcher.fetchSingle(Endpoints.FACT);
        CompletableFuture<String> ip = fetcher.fetchSingle(Endpoints.IP);

        return CompletableFuture.allOf(weather, fact, ip)
                .thenApply(v -> new AggregateResponse(
                        weather.join(),
                        fact.join(),
                        ip.join()
                ));
    }
}