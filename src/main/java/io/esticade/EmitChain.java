package io.esticade;

import io.esticade.driver.Connector;

import javax.json.JsonValue;
import java.util.function.Consumer;

class EmitChain {
    private final ServiceParams serviceParams;
    private final Connector connector;
    private final Event emittedEvent;

    EmitChain(String eventName, JsonValue payload, ServiceParams serviceParams, Connector connector) {
        this.emittedEvent = new Event(serviceParams, eventName, payload);
        this.serviceParams = serviceParams;
        this.connector = connector;
    }

    public EmitChain on(String eventName, Consumer<Event> callback) {
        connector.registerListener(emittedEvent.correlationBlock + "." + eventName, null, ev -> {
            Event event = new Event(serviceParams, ev);
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
