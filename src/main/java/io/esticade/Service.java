package io.esticade;

import io.esticade.driver.ConnectionFactory;
import io.esticade.driver.Connector;
import java.io.IOException;
import java.util.function.Consumer;

public class Service {
    private ServiceParams params;
    private Connector connector;

    public Service(String serviceName) throws IOException {
        params = new ServiceParams(serviceName);
        connector = ConnectionFactory.getConnection();
    }

    public void emit(String eventName, Object payload){
        connector.emit(new Event(params, eventName, payload));
    }

    public void emit(String eventName) {
        connector.emit(new Event(params, eventName, null));
    }

    public void on(String eventName, Consumer<Event> callback) {
        connector.registerListener("*." + eventName, params.serviceName + "-" + eventName, obj -> callback.accept(new Event(params, obj)));
    }

    public void shutdown() {
        ConnectionFactory.shutdown();
    }

    public void alwaysOn(String eventName, Consumer<Event> callback) {
        connector.registerListener("*." + eventName, null, obj -> callback.accept(new Event(params, obj)));
    }

    public EmitChain emitChain(String eventName, Object payload) {
        return new EmitChain(eventName, payload, params, connector);
    }

    public EmitChain emitChain(String eventName) {
        return new EmitChain(eventName, null, params, connector);
    }
}
