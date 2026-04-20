package tsg.rest.aggregator.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;

public record AggregateResponse(
        @JsonRawValue String weather,
        @JsonRawValue String fact,
        @JsonRawValue String ip
) {
}