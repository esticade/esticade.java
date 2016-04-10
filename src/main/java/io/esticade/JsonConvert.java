package io.esticade;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;


class JsonConvert {
    static JsonValue toJsonValue(String payload) {
        JsonObject obj = Json.createObjectBuilder()
            .add("body", payload)
            .build();

        return obj.getJsonString("body");
    }

    static JsonValue toJsonValue(int payload) {
        JsonObject obj = Json.createObjectBuilder()
                .add("body", payload)
                .build();

        return obj.getJsonNumber("body");
    }

    static JsonValue toJsonValue(double payload) {
        JsonObject obj = Json.createObjectBuilder()
                .add("body", payload)
                .build();

        return obj.getJsonNumber("body");
    }

    static JsonValue toJsonValue(float payload) {
        JsonObject obj = Json.createObjectBuilder()
                .add("body", payload)
                .build();

        return obj.getJsonNumber("body");
    }
}
