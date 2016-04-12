package io.esticade.driver;
import com.fasterxml.jackson.databind.JsonNode;
import io.esticade.Event;
import java.util.function.Consumer;

public interface Connector {
    void emit(Event event);
    String registerListener(String routingKey, String queueName, Consumer<JsonNode> callback);
    void shutdown();
    void deleteListener(String tag);
}
