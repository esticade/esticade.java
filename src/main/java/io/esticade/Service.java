package io.esticade;

import io.esticade.driver.ConnectionFactory;
import io.esticade.driver.Connector;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.IOException;
import java.util.function.Consumer;

public class Service {
    private String serviceName;
    private Connector connector;

    public Service(String serviceName) throws IOException {
        this.serviceName = serviceName;
        connector = ConnectionFactory.getConnection();
    }

    public void emit(String eventName, JsonValue payload){
        connector.emit(new Event(serviceName, eventName, payload));
    }

    public void emit(String eventName, String payload){
        connector.emit(new Event(serviceName, eventName, JsonConvert.toJsonValue(payload)));
    }

    public void emit(String eventName, int payload) {
        connector.emit(new Event(serviceName, eventName, JsonConvert.toJsonValue(payload)));
    }

    public void emit(String eventName, double payload) {
        connector.emit(new Event(serviceName, eventName, JsonConvert.toJsonValue(payload)));
    }

    public void on(String eventName, Consumer<Event> callback) {
        connector.registerListener("*." + eventName, serviceName + "-" + eventName, (JsonObject obj) -> callback.accept(new Event(serviceName, obj)));
    }


}
