package tsg.rest.aggregator;

import java.util.concurrent.CompletableFuture;

import tsg.rest.aggregator.dto.AggregateResponse;
import tsg.rest.aggregator.restclient.FetcherService;

public class AggregateService {

    private final FetcherService weatherFetcher;
    private final FetcherService factFetcher;
    private final FetcherService ipFetcher;

    private static final String WEATHER_URL = "https://api.open-meteo.com/v1/forecast?latitude=51.107883&longitude=17.038538&current_weather=true";
    private static final String RANDOM_FACT_URL = "https://uselessfacts.jsph.pl/api/v2/facts/random";
    private static final String IP_URL = "https://api.ipify.org/?format=json";

    public AggregateService() {
        this.weatherFetcher = new FetcherService("weather", WEATHER_URL);
        this.factFetcher = new FetcherService("fact", RANDOM_FACT_URL);
        this.ipFetcher = new FetcherService("ip", IP_URL);
    }

    public CompletableFuture<AggregateResponse> getAggregatedData() {
        CompletableFuture<String> weather = weatherFetcher.fetchSingle();
        CompletableFuture<String> fact = factFetcher.fetchSingle();
        CompletableFuture<String> ip = ipFetcher.fetchSingle();

        return CompletableFuture.allOf(weather, fact, ip)
                .thenApply(v -> new AggregateResponse(
                        weather.join(),
                        fact.join(),
                        ip.join()
                ));
    }
}