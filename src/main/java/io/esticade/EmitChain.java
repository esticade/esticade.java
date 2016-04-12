package io.esticade;

import io.esticade.driver.Connector;

import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

class EmitChain {
    private final ServiceParams serviceParams;
    private final Connector connector;
    private final Event emittedEvent;

    private int timeout = 60000;
    private List<String> ctags = new ArrayList<>();
    private Timer timer = new Timer();

    EmitChain(String eventName, JsonValue payload, ServiceParams serviceParams, Connector connector) {
        this.emittedEvent = new Event(serviceParams, eventName, payload);
        this.serviceParams = serviceParams;
        this.connector = connector;
    }

    public EmitChain on(String eventName, Consumer<Event> callback) {
        ctags.add(connector.registerListener(emittedEvent.correlationBlock + "." + eventName, null, ev -> {
            Event event = new Event(serviceParams, ev);
            if(event.correlationId.equals(emittedEvent.correlationId)) {
                callback.accept(event);
            }
        }));
        return this;
    }

    public void execute() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                terminate();
            }
        }, timeout);
        connector.emit(emittedEvent);
    }

    private void terminate() {
        ctags.forEach((tag) -> connector.deleteListener(tag));
    }

    public EmitChain timeout(int timeoutMsec) {
        timeout = timeoutMsec;
        return this;
    }
}
