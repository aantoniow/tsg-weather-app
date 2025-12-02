package tsg.rest.aggregator.dto;

public record AggregateResponse(
        String weather,
        String fact,
        String ip
) {
}
