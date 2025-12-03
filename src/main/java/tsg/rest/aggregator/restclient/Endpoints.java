package tsg.rest.aggregator.restclient;

public enum Endpoints {
    WEATHER(
            "weather",
            "https://api.open-meteo.com/v1/forecast?latitude=51.107883&longitude=17.038538&current_weather=true"),
    FACT(
            "fact",
            "https://uselessfacts.jsph.pl/api/v2/facts/random"),
    IP(
            "ip",
            "https://api.ipify.org/?format=json");

    private final String key;
    private final String url;

    Endpoints(String key, String url) {
        this.key = key;
        this.url = url;
    }

    public String key() {
        return key;
    }

    public String url() {
        return url;
    }
}