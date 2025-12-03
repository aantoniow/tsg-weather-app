package tsg.rest.aggregator;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.JsonNodeException;
import tools.jackson.databind.node.ObjectNode;

public class JsonUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String toJson(String key, String rawBody) {
        try {
            JsonNode parsed = mapper.readTree(rawBody);
            ObjectNode wrapper = mapper.createObjectNode();
            wrapper.set(key, parsed);
            return mapper.writeValueAsString(wrapper);
        } catch (JsonNodeException e) {
            return String.format("{\"%s\":{\"error\":\"serialization failed\",\"message\":\"%s\"}}",
                    key, e.getMessage().replace("\"", "\\\""));
        }
    }

}