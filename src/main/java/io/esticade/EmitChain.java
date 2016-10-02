package io.esticade;

import io.esticade.driver.Connector;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class EmitChain {
    private final ServiceParams serviceParams;
    private final Connector connector;
    private final Event emittedEvent;

    private int timeout = 60000;
    private List<String> cTags = new ArrayList<>();

    private Consumer<Event> timeOutCallback;

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
        Timer timer = connector.getTimer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                terminate(timer);
                if(timeOutCallback != null){
                    timeOutCallback.accept(emittedEvent);
                }
            }
        }, timeout);
        connector.emit(emittedEvent);
    }

    private void terminate(Timer timer) {
        connector.clearTimer(timer);
        cTags.forEach((tag) -> connector.deleteListener(tag));
    }

    /**
     * Set the timeout in milliseconds after which the chain expires and no more events are listened for.
     * @param timeoutMSec Time in milliseconds
     * @return Current chain
     */
    public EmitChain timeout(int timeoutMSec) {
        timeout = timeoutMSec;
        return this;
    }

    /**
     * Register a callback which is called once the emit chain times out.
     * @param callback - Callback function that will be called when the chain times out. Argument is the original event emitted.
     * @return Current chain
     */
    public EmitChain timeout(Consumer<Event> callback) {
        timeOutCallback = callback;
        return this;
    }

    /**
     * Register a callback which is called once the emit chain times out.*
     * @param timeoutMSec Time in milliseconds
     * @param callback - Callback function that will be called when the chain times out. Argument is the original event emitted.
     * @return Current chain
     */
    public EmitChain timeout(int timeoutMSec, Consumer<Event> callback) {
        return timeout(callback).
            timeout(timeoutMSec);
    }

}
