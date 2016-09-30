package io.esticade;

import io.esticade.driver.ConnectionFactory;
import io.esticade.driver.Connector;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Main class for working with the Esticade.
 */
public class Service {
    private ServiceParams params;
    private Connector connector;

    /**
     * Create a new service connector.
     *
     * @param serviceName Name of the current service as plain text string
     * @throws IOException
     */
    public Service(String serviceName) throws IOException {
        params = new ServiceParams(serviceName);
        connector = ConnectionFactory.getConnection();
    }

    /**
     * Emit message with payload.
     *
     * <p>Only use this method for original events. If event is caused by another event received,
     * please use {@link Event#emit(String, Object)}</p>
     *
     * @param eventName Name of the event as plain text string
     * @param payload Serializable java object that will be sent to the event network.
     */
    public void emit(String eventName, Object payload){
        connector.emit(new Event(params, eventName, payload));
    }

    /**
     * Emit message without payload.
     *
     * <p>Only use this method for original events. If event is caused by another event received,
     * please use {@link Event#emit(String)}</p>
     *
     * @param eventName Name of the event as plain text string
     */
    public void emit(String eventName) {
        connector.emit(new Event(params, eventName, null));
    }

    /**
     * Register shared persistent event handler.
     *
     * <p>If there are more than one instance of the same service, the events will be divided between instances using
     * round-robin algorithm. If "engraved" option is enabled, the queues will remain in exchange while the service
     * is inactive and messages are accepted once service reconnects.</p>
     *
     * @param eventName Name of the event to listen to as a plain text string.
     * @param callback Callback with single argument that will be called once the event is received.
     */
    public void on(String eventName, Consumer<Event> callback) {
        connector.registerListener("*." + eventName, params.serviceName + "-" + eventName, obj -> callback.accept(new Event(params, obj)));
    }

    /**
     * Disconnect service from the event network
     */
    public void shutdown() {
        ConnectionFactory.shutdown();
    }

    /**
     * Register temporary non-shared event handler.
     *
     * <p>If there are more than one instance of the same service, all the events will be received in all instances.
     * The "engraved" option is ignored, these queues are only active while the instance is active.</p>
     *
     * @param eventName Name of the event to listen to as a plain text string.
     * @param callback Callback with single argument that will be called once the event is received.
     */
    public void alwaysOn(String eventName, Consumer<Event> callback) {
        connector.registerListener("*." + eventName, null, obj -> callback.accept(new Event(params, obj)));
    }

    /**
     * Emit an event and listen for events caused by this event.
     *
     * <p>Will trigger an event and return {@link EmitChain} object which allows user to register event handlers that
     * only trigger on events that are caused by the event triggered from the emitChain mehtod.</p>
     *
     * <p>Please note that the actual event will only be triggered once the {@link EmitChain#execute()} method is
     * called</p>
     *
     * @param eventName Name of the event as plain text string
     * @param payload Serializable java object that will be sent to the event network.
     * @return Emission chain object, allowing to register event handlers via fluent interface.
     */
    public EmitChain emitChain(String eventName, Object payload) {
        return new EmitChain(eventName, payload, params, connector);
    }

    /**
     * Emit an event and listen for events caused by this event.
     *
     * <p>Same as {@link #emitChain(String, Object)} except for events without payload.</p>
     * @param eventName Name of the event as plain text string
     * @return Emission chain object, allowing to register event handlers via fluent interface.
     */
    public EmitChain emitChain(String eventName) {
        return new EmitChain(eventName, null, params, connector);
    }
}
