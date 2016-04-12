package io.esticade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.esticade.driver.ConnectionFactory;

import java.io.IOException;
import java.util.UUID;

public final class Event {
    public final String correlationId;
    public final String correlationBlock;
    public final String eventId;
    public final String parentId;
    public final String service;

    public final String name;
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

    public void emit(String eventName, Object payload) {
        try {
            ConnectionFactory.getConnection()
                .emit(new Event(serviceParams, eventName, payload, this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void emit(String eventName) {
        emit(eventName, null);
    }

    public <T> T bodyAs(Class<T> testBeanClass) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(body, testBeanClass);
    }
}
