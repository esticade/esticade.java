package io.esticade;

import io.esticade.driver.Connector;

import javax.json.JsonValue;
import java.util.function.Consumer;

class EmitChain {
    private final String serviceName;
    private final Connector connector;
    private final Event emittedEvent;

    EmitChain(String eventName, JsonValue payload, String serviceName, Connector connector) {
        this.emittedEvent = new Event(serviceName, eventName, payload);
        this.serviceName = serviceName;
        this.connector = connector;
    }

    public EmitChain on(String eventName, Consumer<Event> callback) {
        connector.registerListener(emittedEvent.correlationBlock + "." + eventName, null, ev -> {
            Event event = new Event(serviceName, ev);
            if(event.correlationId.equals(emittedEvent.correlationId)) {
                callback.accept(event);
            }
        });
        return this;
    }

    public void execute() {
        connector.emit(emittedEvent);
    }
}
