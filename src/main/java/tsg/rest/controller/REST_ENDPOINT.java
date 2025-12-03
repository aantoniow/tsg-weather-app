package tsg.rest.controller;

public enum REST_ENDPOINT {
    WEATHER_URL("weather", "https://api.open-meteo.com/v1/forecast?latitude=51.107883&longitude=17.038538&current_weather=true"),
    RANDOM_URL("random", "https://uselessfacts.jsph.pl/api/v2/facts/random"),
    IP_URL("ip", "https://api.ipify.org/?format=json");

    private final String key;
    private final String value;

    REST_ENDPOINT(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
