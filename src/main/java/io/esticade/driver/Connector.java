package io.esticade.driver;
import io.esticade.Event;
import io.esticade.Service;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.function.Consumer;

public interface Connector {
    void emit(Event event);
    String registerListener(String routingKey, String queueName, Consumer<JsonObject> callback);
    void shutdown();
    void deleteListener(String tag);
}
