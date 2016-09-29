package io.esticade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.esticade.driver.ConnectionFactory;

import java.io.IOException;
import java.util.UUID;

public final class Event {
    /**
     * Correlation ID used for connecting events into chains.
     */
    public final String correlationId;

    /**
     * Correlation block, used for similar purposes for more optimized routing.
     */
    public final String correlationBlock;

    /**
     * Event unique ID
     */
    public final String eventId;

    /**
     * Event ID of an event that caused the current event.
     */
    public final String parentId;

    /**
     * Service name that triggered the event
     */
    public final String service;

    /**
     * Event name
     */
    public final String name;

    /**
     * Object body. For more friendlier interface use {@link #bodyAs(Class)} method.
     */
    public final Object body;

    private ServiceParams serviceParams;

    Event(ServiceParams serviceParams, String eventId, String name, Object payload, String correlationId, String correlationBlock, String parentId){
        this.name = name;
        this.body = payload;
        this.serviceParams = serviceParams;
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.correlationBlock = correlationBlock;
        this.parentId = parentId;
        this.service = serviceParams.serviceName;
    }

    Event(ServiceParams serviceParams, String name, Object payload){
        this(serviceParams, UUID.randomUUID().toString(), name, payload, UUID.randomUUID().toString(), serviceParams.correlationBlock, null);
    }

    private Event(ServiceParams serviceParams, String name, Object payload, Event parentEvent){
        this(serviceParams, UUID.randomUUID().toString(), name, payload, parentEvent.correlationId, parentEvent.correlationBlock, parentEvent.eventId);
    }

    Event(ServiceParams serviceParams, JsonNode obj) {
        this(
            serviceParams,
            obj.get("eventId").asText(),
            obj.get("name").asText(),
            toAppropriateType(obj.get("body")),
            obj.get("correlationId").asText(),
            obj.get("correlationBlock").asText(),
            null
        );
    }

    private static Object toAppropriateType(JsonNode body) {
        switch(body.getNodeType()){
            case BOOLEAN: return body.asBoolean();
            case NULL: return null;
            case NUMBER:
                Object result;
                if(body.isInt()) {
                    result = body.asInt();
                } else {
                    result = body.asDouble();
                }
                return result;
            case STRING: return body.asText();
            default: return body;
        }
    }

    /**
     * Get string representation of the object
     * @return String representation of the object.
     */
    @Override
    public String toString() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Emit an event with payload
     *
     * <p>Always use this method when the triggered event is caused by the event received.</p>
     *
     * @param eventName
     * @param payload
     */
    public void emit(String eventName, Object payload) {
        try {
            ConnectionFactory.getConnection()
                .emit(new Event(serviceParams, eventName, payload, this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Emit event without payload.
     *
     * <p>Always use this method when the triggered event is caused by the event received.</p>
     *
     * @param eventName
     */
    public void emit(String eventName) {
        emit(eventName, null);
    }

    /**
     * Map received event json to a java bean.
     *
     * @param testBeanClass Class type of the received event body
     * @return Event body mapped into a bean.
     */
    public <T> T bodyAs(Class<T> testBeanClass) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(body, testBeanClass);
    }
}
