package io.esticade;

import io.esticade.driver.ConnectionFactory;
import io.esticade.driver.Connector;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.IOException;
import java.util.function.Consumer;

public class Service {
    private ServiceParams params;
    private Connector connector;

    public Service(String serviceName) throws IOException {
        params = new ServiceParams(serviceName);
        connector = ConnectionFactory.getConnection();
    }

    public void emit(String eventName, JsonValue payload){
        connector.emit(new Event(params, eventName, payload));
    }

    public void emit(String eventName, String payload){
        connector.emit(new Event(params, eventName, JsonConvert.toJsonValue(payload)));
    }

    public void emit(String eventName, int payload) {
        connector.emit(new Event(params, eventName, JsonConvert.toJsonValue(payload)));
    }

    public void emit(String eventName, double payload) {
        connector.emit(new Event(params, eventName, JsonConvert.toJsonValue(payload)));
    }

    public void emit(String eventName, boolean payload) {
        connector.emit(new Event(params, eventName, JsonConvert.toJsonValue(payload)));
    }

    public void emit(String eventName) {
        connector.emit(new Event(params, eventName, null));
    }

    public void on(String eventName, Consumer<Event> callback) {
        connector.registerListener("*." + eventName, params.serviceName + "-" + eventName, (JsonObject obj) -> callback.accept(new Event(params, obj)));
    }

    public void shutdown() {
        ConnectionFactory.shutdown();
    }

    public void alwaysOn(String eventName, Consumer<Event> callback) {
        connector.registerListener("*." + eventName, null, (JsonObject obj) -> callback.accept(new Event(params, obj)));

    }

    public EmitChain emitChain(String eventName, JsonValue payload) {
        return new EmitChain(eventName, payload, params, connector);
    }

    public EmitChain emitChain(String eventName) {
        return new EmitChain(eventName, JsonValue.NULL, params, connector);
    }
}
