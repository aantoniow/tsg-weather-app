package tsg.rest.json;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Optional;

public class JsonHelper {
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String EMPTY_KEY = "error";
    private static final String EMPTY_MSG = "null response";

    public static String addNodeToKey(String key, String jsonNodeStr) {
        ObjectNode objectNode = mapper.createObjectNode();
        JsonNode jsonNode = mapper.readTree(Optional.ofNullable(jsonNodeStr).orElse("{\"" +EMPTY_KEY + "\":\"" + EMPTY_MSG + "\"}"));
        objectNode.set(key, jsonNode);

        return objectNode.toString();
    }
}
