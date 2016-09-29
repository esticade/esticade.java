package io.esticade;

import io.esticade.driver.Connector;

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
    private List<String> cTags = new ArrayList<>();
    private Timer timer = new Timer();

    EmitChain(String eventName, Object payload, ServiceParams serviceParams, Connector connector) {
        this.emittedEvent = new Event(serviceParams, eventName, payload);
        this.serviceParams = serviceParams;
        this.connector = connector;
    }



    /**
     * Register exclusive temporary non-shared event handler.
     *
     * <p>Will only receive events that are caused by the same event that created the chain.</p>
     *
     * @param eventName Name of the event to listen to as a plain text string.
     * @param callback Callback with single argument that will be called once the event is received.
     */

    public EmitChain on(String eventName, Consumer<Event> callback) {
        cTags.add(connector.registerListener(emittedEvent.correlationBlock + "." + eventName, null, ev -> {
            Event event = new Event(serviceParams, ev);
            if(event.correlationId.equals(emittedEvent.correlationId)) {
                callback.accept(event);
            }
        }));
        return this;
    }

    /**
     * Trigger the event chain.
     *
     * <p>Please not that this method will trigger the actual event after it is called and all requested event
     * listeners are finalized.</p>
     */
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
        cTags.forEach((tag) -> connector.deleteListener(tag));
    }

    /**
     * Set the timeout in milliseconds after which the chain expires and no more events are listened for.
     * @param timeoutMSec
     * @return
     */
    public EmitChain timeout(int timeoutMSec) {
        timeout = timeoutMSec;
        return this;
    }
}
